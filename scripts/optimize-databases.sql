-- Database Performance Optimization Script
-- This script adds additional indexes and optimizations for better query performance

-- ============================================
-- AUTH SERVICE DATABASE OPTIMIZATIONS
-- ============================================

\c auth_service;

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email, email_verified);
CREATE INDEX IF NOT EXISTS idx_users_email_locked ON users(email, account_locked);

-- Index for session cleanup queries
CREATE INDEX IF NOT EXISTS idx_user_sessions_active_expires ON user_sessions(is_active, expires_at);

-- Partial index for active sessions only (more efficient)
CREATE INDEX IF NOT EXISTS idx_user_sessions_active_only ON user_sessions(user_id, expires_at) WHERE is_active = true;

-- Index for token cleanup queries
CREATE INDEX IF NOT EXISTS idx_reset_tokens_unused_expires ON password_reset_tokens(is_used, expires_at);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_unused_expires ON email_verification_tokens(used, expires_at);

-- Analyze tables for query planner
ANALYZE users;
ANALYZE user_sessions;
ANALYZE password_reset_tokens;
ANALYZE email_verification_tokens;

-- ============================================
-- EVENT SERVICE DATABASE OPTIMIZATIONS
-- ============================================

\c event_service;

-- Composite indexes for common search patterns
CREATE INDEX IF NOT EXISTS idx_events_status_date ON events(status, event_date) WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_events_category_date ON events(category_id, event_date) WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_events_date_status ON events(event_date, status) WHERE event_date > CURRENT_TIMESTAMP;

-- Index for organizer dashboard queries
CREATE INDEX IF NOT EXISTS idx_events_organizer_status ON events(organizer_id, status, created_at DESC);

-- Partial index for published events only (most common query)
CREATE INDEX IF NOT EXISTS idx_events_published_date ON events(event_date, name) WHERE status = 'PUBLISHED';

-- Index for price range queries
CREATE INDEX IF NOT EXISTS idx_events_price_date ON events(min_price, max_price, event_date) WHERE status = 'PUBLISHED';

-- Covering index for event list queries (includes commonly selected columns)
CREATE INDEX IF NOT EXISTS idx_events_list_covering ON events(status, event_date, category_id) 
    INCLUDE (name, min_price, max_price, image_url) WHERE status = 'PUBLISHED';

-- Full-text search optimization
CREATE INDEX IF NOT EXISTS idx_events_name_trgm ON events USING gin(name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_events_description_trgm ON events USING gin(description gin_trgm_ops);

-- Venue search optimization
CREATE INDEX IF NOT EXISTS idx_venues_name_trgm ON venues USING gin(name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_venues_city_trgm ON venues USING gin(city gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_venues_city_state ON venues(city, state);

-- Category optimization
CREATE INDEX IF NOT EXISTS idx_categories_name_trgm ON event_categories USING gin(name gin_trgm_ops);

-- Enable pg_trgm extension for fuzzy text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Analyze tables for query planner
ANALYZE events;
ANALYZE venues;
ANALYZE event_categories;

-- ============================================
-- TICKET SERVICE DATABASE OPTIMIZATIONS
-- ============================================

\c ticket_service;

-- Composite indexes for inventory queries
CREATE INDEX IF NOT EXISTS idx_ticket_types_event_available ON ticket_types(event_id, quantity_available) 
    WHERE quantity_available > 0;

-- Index for reservation expiration cleanup
CREATE INDEX IF NOT EXISTS idx_ticket_reservations_expired ON ticket_reservations(reserved_until) 
    WHERE reserved_until < CURRENT_TIMESTAMP;

-- Composite index for user reservations
CREATE INDEX IF NOT EXISTS idx_ticket_reservations_user_reserved ON ticket_reservations(user_id, reserved_until);

-- Index for ticket lookup by QR code (most common validation query)
CREATE INDEX IF NOT EXISTS idx_tickets_qr_status ON tickets(qr_code, status);

-- Index for user ticket queries
CREATE INDEX IF NOT EXISTS idx_tickets_order_status ON tickets(order_id, status);

-- Covering index for ticket list queries
CREATE INDEX IF NOT EXISTS idx_tickets_user_covering ON tickets(order_id, status) 
    INCLUDE (ticket_number, qr_code, holder_name);

-- Analyze tables for query planner
ANALYZE ticket_types;
ANALYZE ticket_reservations;
ANALYZE tickets;

-- ============================================
-- PAYMENT SERVICE DATABASE OPTIMIZATIONS
-- ============================================

\c payment_service;

-- Composite indexes for order queries
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, payment_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_event_status ON orders(event_id, payment_status);

-- Index for order number lookup (unique but explicit index helps)
CREATE INDEX IF NOT EXISTS idx_orders_number_status ON orders(order_number, payment_status);

-- Partial index for pending orders (for timeout processing)
CREATE INDEX IF NOT EXISTS idx_orders_pending ON orders(created_at) WHERE payment_status = 'PENDING';

-- Index for transaction reconciliation
CREATE INDEX IF NOT EXISTS idx_payment_transactions_gateway_status ON payment_transactions(gateway_transaction_id, status);

-- Composite index for order items
CREATE INDEX IF NOT EXISTS idx_order_items_order_ticket ON order_items(order_id, ticket_type_id);

-- Covering index for order history queries
CREATE INDEX IF NOT EXISTS idx_orders_user_history ON orders(user_id, created_at DESC) 
    INCLUDE (order_number, total_amount, payment_status);

-- Analyze tables for query planner
ANALYZE orders;
ANALYZE order_items;
ANALYZE payment_transactions;

-- ============================================
-- NOTIFICATION SERVICE DATABASE OPTIMIZATIONS
-- ============================================

\c notification_service;

-- Index for pending notifications
CREATE INDEX IF NOT EXISTS idx_notifications_pending ON notifications(status, created_at) WHERE status = 'PENDING';

-- Index for delivery status tracking
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status, created_at DESC);

-- Index for failed notifications (for retry processing)
CREATE INDEX IF NOT EXISTS idx_notifications_failed ON notifications(created_at) WHERE status = 'FAILED';

-- Index for template lookup
CREATE INDEX IF NOT EXISTS idx_notification_templates_name ON notification_templates(name);

-- Analyze tables for query planner
ANALYZE notifications;
ANALYZE notification_templates;

-- ============================================
-- VACUUM AND MAINTENANCE
-- ============================================

-- Run VACUUM ANALYZE on all databases to reclaim space and update statistics
\c auth_service;
VACUUM ANALYZE;

\c event_service;
VACUUM ANALYZE;

\c ticket_service;
VACUUM ANALYZE;

\c payment_service;
VACUUM ANALYZE;

\c notification_service;
VACUUM ANALYZE;

-- ============================================
-- QUERY PERFORMANCE MONITORING
-- ============================================

-- Enable pg_stat_statements extension for query performance monitoring
\c auth_service;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

\c event_service;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

\c ticket_service;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

\c payment_service;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

\c notification_service;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
