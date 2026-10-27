import fs from 'node:fs';
import path from 'node:path';
import { KnowledgeGraphSchema } from '/Users/jxin/.claude/plugins/cache/understand-anything/understand-anything/2.9.8/packages/core/dist/index.js';

const graphPath = process.argv[2];
const scanPath = process.argv[3];
const outputPath = process.argv[4];
const graph = JSON.parse(fs.readFileSync(graphPath, 'utf8'));
const scan = JSON.parse(fs.readFileSync(scanPath, 'utf8'));
const result = KnowledgeGraphSchema.safeParse(graph);
const fileLevelTypes = new Set(['file', 'config', 'document', 'service', 'pipeline', 'table', 'schema', 'resource', 'endpoint']);
const inventory = new Map(scan.files.map(file => [file.path, file]));
const covered = new Set(graph.nodes.filter(node => fileLevelTypes.has(node.type)).map(node => node.filePath).filter(Boolean));
const missingFiles = [...inventory.keys()].filter(file => !covered.has(file));
const unexpectedFiles = [...new Set(graph.nodes.map(node => node.filePath).filter(file => file && !inventory.has(file)))];
const invalidLineRanges = graph.nodes.filter(node => {
  if (!node.lineRange || !inventory.has(node.filePath)) return false;
  const [start, end] = node.lineRange;
  return !Number.isInteger(start) || !Number.isInteger(end) || start < 1 || end < start || end > inventory.get(node.filePath).sizeLines;
}).map(node => ({ id: node.id, lineRange: node.lineRange, sizeLines: inventory.get(node.filePath).sizeLines }));
const report = {
  success: result.success && missingFiles.length === 0 && unexpectedFiles.length === 0 && invalidLineRanges.length === 0,
  schemaIssues: result.success ? [] : result.error.issues,
  missingFiles,
  unexpectedFiles,
  invalidLineRanges,
  analyzedFiles: inventory.size,
  coveredFiles: [...inventory.keys()].filter(file => covered.has(file)).length,
};
fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + '\n');
process.stdout.write(JSON.stringify({ ...report, schemaIssues: report.schemaIssues.length, missingFiles: report.missingFiles.length, unexpectedFiles: report.unexpectedFiles.length, invalidLineRanges: report.invalidLineRanges.length }) + '\n');
process.exitCode = report.success ? 0 : 1;
