CREATE TABLE model_groups (
    id BIGINT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    name VARCHAR(100) NOT NULL,
    model_whitelist TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_model_groups_owner_name_active
    ON model_groups(owner_user_id, name)
    WHERE deleted = FALSE;

INSERT INTO model_groups (id, owner_user_id, name, model_whitelist, created_at, updated_at, deleted)
SELECT id,
       owner_user_id,
       LEFT(name, 64) || '（迁移分组-' || id || '）',
       model_whitelist,
       created_at,
       updated_at,
       FALSE
FROM api_credentials
WHERE deleted = FALSE;

ALTER TABLE api_credentials
    ADD COLUMN model_group_id BIGINT;

UPDATE api_credentials
SET model_group_id = id
WHERE deleted = FALSE;

-- Deleted credentials are historical only; give them an archived group so the new FK remains valid.
INSERT INTO model_groups (id, owner_user_id, name, model_whitelist, created_at, updated_at, deleted)
SELECT id,
       owner_user_id,
       LEFT(name, 64) || '（已删除迁移分组-' || id || '）',
       model_whitelist,
       created_at,
       updated_at,
       TRUE
FROM api_credentials
WHERE deleted = TRUE;

UPDATE api_credentials
SET model_group_id = id
WHERE deleted = TRUE;

ALTER TABLE api_credentials
    ALTER COLUMN model_group_id SET NOT NULL,
    ADD CONSTRAINT fk_api_credentials_model_group
        FOREIGN KEY (model_group_id) REFERENCES model_groups(id),
    DROP COLUMN model_whitelist;

CREATE INDEX idx_api_credentials_model_group
    ON api_credentials(model_group_id)
    WHERE deleted = FALSE;
