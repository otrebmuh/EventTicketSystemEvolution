-- Create notification_service database and user
CREATE DATABASE notification_service;
CREATE USER notification_user WITH ENCRYPTED PASSWORD 'notification_password';
GRANT ALL PRIVILEGES ON DATABASE notification_service TO notification_user;

-- Connect to notification_service database
\c notification_service;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO notification_user;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Notification templates with versioning
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    html_content TEXT NOT NULL,
    text_content TEXT NOT NULL,
    category VARCHAR(50) CHECK (category IN (
        'TRANSACTIONAL', 'PROMOTIONAL', 'SYSTEM', 'REMINDER'
    )),
    version INTEGER DEFAULT 1 NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for template queries
CREATE INDEX idx_templates_name ON notification_templates(name);
CREATE INDEX idx_templates_category ON notification_templates(category);
CREATE INDEX idx_templates_active ON notification_templates(is_active) WHERE is_active = TRUE;

-- Notifications with delivery tracking
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    template_id UUID REFERENCES notification_templates(id),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    html_content TEXT,
    text_content TEXT,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'QUEUED', 'PROCESSING', 'SENT', 'DELIVERED', 
        'FAILED', 'BOUNCED', 'COMPLAINED', 'PERMANENTLY_FAILED'
    )),
    channel VARCHAR(20) DEFAULT 'EMAIL' CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WEB', 'MOBILE')),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    error_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notification queries
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_email);
CREATE INDEX idx_notifications_retry ON notifications(status, retry_count) 
    WHERE status IN ('FAILED', 'PENDING') AND retry_count < max_retries;

-- Delivery status tracking
CREATE TABLE delivery_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_message_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(50),
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for delivery tracking
CREATE INDEX idx_delivery_notification ON delivery_status(notification_id, event_timestamp DESC);
CREATE INDEX idx_delivery_provider_id ON delivery_status(provider_message_id);
CREATE INDEX idx_delivery_event_type ON delivery_status(event_type);

-- Insert default email templates
INSERT INTO notification_templates (name, subject, html_content, text_content, category, created_by) VALUES
(
    'EMAIL_VERIFICATION',
    'Verify Your Email Address',
    '<html><body><h1>Welcome!</h1><p>Please verify your email by clicking the link below:</p><p><a href="{{verificationLink}}">Verify Email</a></p></body></html>',
    'Welcome! Please verify your email by visiting: {{verificationLink}}',
    'TRANSACTIONAL',
    '00000000-0000-0000-0000-000000000000'
),
(
    'PASSWORD_RESET',
    'Reset Your Password',
    '<html><body><h1>Password Reset</h1><p>Click the link below to reset your password:</p><p><a href="{{resetLink}}">Reset Password</a></p><p>This link expires in 15 minutes.</p></body></html>',
    'Reset your password by visiting: {{resetLink}}. This link expires in 15 minutes.',
    'TRANSACTIONAL',
    '00000000-0000-0000-0000-000000000000'
),
(
    'ORDER_CONFIRMATION',
    'Order Confirmation - {{orderNumber}}',
    '<html><body><h1>Order Confirmed!</h1><p>Hi {{customerName}},</p><p>Your order {{orderNumber}} has been confirmed.</p><p>Total: {{totalAmount}}</p><p>Event: {{eventName}}</p><p>Date: {{eventDate}}</p></body></html>',
    'Hi {{customerName}}, Your order {{orderNumber}} has been confirmed. Total: {{totalAmount}}. Event: {{eventName}} on {{eventDate}}.',
    'TRANSACTIONAL',
    '00000000-0000-0000-0000-000000000000'
),
(
    'TICKET_DELIVERY',
    'Your Event Tickets',
    '<html><body><h1>Your Event Tickets</h1><p>Thank you for your purchase! Your tickets are ready.</p><p>Please check the attached tickets or use the links provided in the email.</p></body></html>',
    'Your Event Tickets - Thank you for your purchase! Your tickets are ready. Please check the attached tickets or use the links provided in the email.',
    'TRANSACTIONAL',
    '00000000-0000-0000-0000-000000000000'
);
