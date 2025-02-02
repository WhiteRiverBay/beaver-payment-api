CREATE TABLE wrb_address_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    encrypted_address VARCHAR(128) NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    uid VARCHAR(64),
    chain_type SMALLINT(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL
);

-- Add indexes for better query performance
CREATE INDEX idx_address_pool_used ON address_pool(used);
CREATE INDEX idx_address_pool_uid ON address_pool(uid);
CREATE INDEX idx_address_pool_chain_type ON address_pool(chain_type);