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
CREATE INDEX idx_address_pool_used ON wrb_address_pool(used);
CREATE INDEX idx_address_pool_uid ON wrb_address_pool(uid);
CREATE INDEX idx_address_pool_chain_type ON wrb_address_pool(chain_type);

-- Modify logo column length in wrb_payment_order table
ALTER TABLE wrb_payment_order MODIFY COLUMN logo VARCHAR(200);
