-- Migration script for offline messaging support
-- Add delivery tracking columns to messages table

ALTER TABLE messages 
ADD COLUMN delivered BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN delivered_at TIMESTAMP NULL;

-- Create index for faster undelivered message queries
CREATE INDEX idx_messages_delivered ON messages(receiver_id, delivered) WHERE delivered = false;

-- Update existing messages to mark them as delivered (optional - depends on your requirements)
-- UPDATE messages SET delivered = true, delivered_at = timestamp WHERE delivered = false;

-- Migration for group message delivery tracking
-- Create table to track delivery status for each group member

CREATE TABLE group_message_delivery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT false,
    delivered_at TIMESTAMP NULL,
    CONSTRAINT fk_group_message_delivery_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_message_delivery_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for faster queries
CREATE INDEX idx_group_message_delivery_user_delivered ON group_message_delivery(user_id, delivered) WHERE delivered = false;
CREATE INDEX idx_group_message_delivery_message ON group_message_delivery(message_id);
CREATE INDEX idx_group_message_delivery_user ON group_message_delivery(user_id);

-- Create unique constraint to prevent duplicate delivery records
CREATE UNIQUE INDEX idx_group_message_delivery_unique ON group_message_delivery(message_id, user_id);
