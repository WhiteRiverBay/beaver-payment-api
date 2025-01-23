ALTER TABLE wrb_trade_log ADD COLUMN confirmed_blocks_required INT DEFAULT -1;
ALTER TABLE wrb_trade_log ADD COLUMN status INT DEFAULT 1;