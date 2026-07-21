package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Detects Claude Code client-specific tool patterns (Workflow, SendMessage/Agent delegation,
 * background task completion) and produces system prompt instructions that guide model behavior.
 *
 * <p>Extracted from BedrockConverseProtocolMessageConverter to separate behavior-policy concerns
 * from protocol-format conversion.
 */
final class ClaudeCodeBehaviorPolicyEnricher {

    private static final String ASK_USER_QUESTION_TOOL = "AskUserQuestion";
    private static final String ENTER_PLAN_MODE_TOOL = "EnterPlanMode";
    private static final String EXIT_PLAN_MODE_TOOL = "ExitPlanMode";
    private static final String WORKFLOW_TOOL = "Workflow";
    private static final String WORKFLOW_PROMPT_PREFIX = "Run the \"";
    private static final String WORKFLOW_INVOKE_DIRECTIVE = "Invoke: Workflow(";
    private static final String SEND_MESSAGE_TOOL = "SendMessage";
    private static final String AGENT_TOOL = "Agent";
    private static final String LEGACY_AGENT_TOOL = "Task";
    private static final String TASK_NOTIFICATION_TAG = "<task-notification>";
    private static final String TASK_COMPLETED_TAG = "<status>completed</status>";
    private static final String TASK_ID_TAG = "<task-id>";
    private static final String WORKFLOW_INVOCATION_INSTRUCTION =
            "The latest user message is a Claude Code workflow dispatch instruction. "
                    + "The workflow has not started merely because the Skill tool returned 'Launching skill'. "
                    + "Invoke the Workflow tool exactly as requested before claiming that it is running.";
    private static final String BACKGROUND_TASK_COMPLETION_INSTRUCTION =
            "A Claude Code task-notification status of 'completed' only means the background agent stopped. "
                    + "Inspect its result and verify that the requested outcome or artifact was actually produced. "
                    + "If the result is progress-only or incomplete, resume the same agent with SendMessage using "
                    + "the task-id; do not report success or duplicate the task in the main agent.";
    private static final String AGENT_SELECTION_INSTRUCTION =
            "Claude Code tool-selection rule: use direct Read, Glob, Grep, or Bash only when the target is "
                    + "already known and the operation is local. For open-ended investigation spanning multiple "
                    + "files, independent verification, or parallelizable work, invoke the %s tool and consume "
                    + "its result instead of repeatedly performing the same investigation in the main agent.";
    private static final String EXPLICIT_AGENT_REQUEST_INSTRUCTION =
            "The user explicitly requested delegation, a subagent, or parallel agent work. Invoke the %s tool "
                    + "for the delegated work before attempting to reproduce that work with direct tools.";
    private static final Pattern EXPLICIT_AGENT_REQUEST_PATTERN = Pattern.compile(
            "(?iu)(sub[ -]?agent|\\bagent\\b|delegate|delegation|parallel agent|"
                    + "子\\s*(?:agent|代理)|委派|代理执行|并行(?:调查|分析|处理|执行|agent))");
    private static final Pattern CLAUDE_SYSTEM_REMINDER_PATTERN = Pattern.compile(
            "(?is)<system-reminder>.*?</system-reminder>");

    private ClaudeCodeBehaviorPolicyEnricher() {
    }

    // ---- public query methods used by the converter ----

    static boolean workflowInvocationRequired(JsonNode tools, JsonNode messages,
                                              Function<JsonNode, String> contentExtractor) {
        return containsToolNamed(tools, WORKFLOW_TOOL)
                && lastUserMessageRequiresWorkflow(messages, contentExtractor);
    }

    static boolean backgroundTaskCompletionRequiresValidation(JsonNode tools, JsonNode messages,
                                                              Function<JsonNode, String> contentExtractor) {
        if (!containsToolNamed(tools, SEND_MESSAGE_TOOL)) {
            return false;
        }
        String text = lastUserMessageText(messages, contentExtractor);
        return text.contains(TASK_NOTIFICATION_TAG)
                && text.contains(TASK_COMPLETED_TAG)
                && text.contains(TASK_ID_TAG);
    }

    static String agentSelectionInstruction(JsonNode tools, JsonNode originalMessages) {
        JsonNode agentTool = findAgentTool(tools);
        if (agentTool == null) {
            return "";
        }
        String toolName = agentTool.path("name").asText(AGENT_TOOL);
        String latestInstruction = CLAUDE_SYSTEM_REMINDER_PATTERN
                .matcher(lastUserInstructionText(originalMessages)).replaceAll(" ");
        boolean explicitlyRequested = EXPLICIT_AGENT_REQUEST_PATTERN.matcher(latestInstruction).find();
        String description = agentTool.path("description").asText("").toLowerCase(Locale.ROOT);
        boolean proactiveUseProhibited = description.contains("do not spawn agents unless the user asks")
                || description.contains("only use this tool when the user explicitly")
                || description.contains("only use this tool when the user asks");
        if (proactiveUseProhibited && !explicitlyRequested) {
            return "";
        }
        if (explicitlyRequested) {
            return EXPLICIT_AGENT_REQUEST_INSTRUCTION.formatted(toolName);
        }
        return AGENT_SELECTION_INSTRUCTION.formatted(toolName);
    }

    static String workflowInvocationInstructionText() {
        return WORKFLOW_INVOCATION_INSTRUCTION;
    }

    static String backgroundTaskCompletionInstructionText() {
        return BACKGROUND_TASK_COMPLETION_INSTRUCTION;
    }

    // ---- internal helpers ----

    private static boolean containsToolNamed(JsonNode tools, String expectedName) {
        if (tools == null || !tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (expectedName.equals(tool.path("name").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode findAgentTool(JsonNode tools) {
        if (tools == null || !tools.isArray()) {
            return null;
        }
        for (JsonNode tool : tools) {
            String name = tool.path("name").asText("");
            if (AGENT_TOOL.equals(name)) {
                return tool;
            }
            JsonNode schema = tool.hasNonNull("input_schema") ? tool.get("input_schema") : tool.get("inputSchema");
            if (LEGACY_AGENT_TOOL.equals(name) && schema != null
                    && schema.path("properties").has("prompt")
                    && schema.path("properties").has("subagent_type")) {
                return tool;
            }
        }
        return null;
    }

    private static String lastUserInstructionText(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return "";
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText(""))) {
                continue;
            }
            JsonNode content = message.get("content");
            if (content == null || content.isNull()) {
                continue;
            }
            if (content.isTextual()) {
                return content.asText("");
            }
            if (content.isArray()) {
                StringBuilder instruction = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText(""))) {
                        if (!instruction.isEmpty()) {
                            instruction.append('\n');
                        }
                        instruction.append(block.path("text").asText(""));
                    }
                }
                if (!instruction.isEmpty()) {
                    return instruction.toString();
                }
            }
        }
        return "";
    }

    private static boolean lastUserMessageRequiresWorkflow(JsonNode messages,
                                                           Function<JsonNode, String> contentExtractor) {
        String text = lastUserMessageText(messages, contentExtractor).stripLeading();
        return text.startsWith(WORKFLOW_PROMPT_PREFIX)
                && text.contains("\" workflow.")
                && text.contains(WORKFLOW_INVOKE_DIRECTIVE);
    }

    private static String lastUserMessageText(JsonNode messages, Function<JsonNode, String> contentExtractor) {
        if (messages == null || !messages.isArray()) {
            return "";
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText(""))) {
                continue;
            }
            return contentExtractor.apply(message.get("content"));
        }
        return "";
    }
}
