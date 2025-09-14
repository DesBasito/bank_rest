INSERT INTO roles (name, description)
VALUES ('USER', 'Обычный пользователь'),
       ('ADMIN', 'Администратор системы');

INSERT INTO users (phone_number, password, first_name, middle_name, last_name, enabled)
VALUES ('+79001234567', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Администратор', 'admin',
        'Системы', true),
       ('+79001234568', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Иван', 'Иванович', 'Иванов',
        true),
       ('+79001234569', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Петр', 'Петрович', 'Петров',
        true),
       ('+79001234570', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Мария', 'Александровна',
        'Сидорова',
        true),
       ('+79001234571', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Анна', 'Сергеевна',
        'Кузнецова',
        false),
       ('+79001234572', '$2a$12$WB2YUbFcCN0tm44SBcKUjua9yiFBsfB3vW02IjuwzY7HGtlQIKzy2', 'Дмитрий', 'Владимирович',
        'Смирнов',
        true);

INSERT INTO user_roles (user_id, role_id)
VALUES (1, 2),
       (2, 1),
       (3, 1),
       (4, 1),
       (5, 1),
       (6, 1);

INSERT INTO cards (card_number, owner_id, expiry_date, type, status, balance)
VALUES ('4111111111111111', 2, DATE '2026-12-31','DEBIT'::card_type, 'ACTIVE'::card_status, 15000.50),
       ('4222222222222222', 2, DATE '2027-06-30','DEBIT'::card_type, 'ACTIVE'::card_status, 5000.00),
       ('4333333333333333', 3, DATE '2026-08-31','DEBIT'::card_type, 'ACTIVE'::card_status, 25000.75),
       ('4444444444444444', 4, DATE '2025-12-31','DEBIT'::card_type, 'EXPIRED'::card_status, 0.00),
       ('4555555555555555', 4, DATE '2027-03-31','DEBIT'::card_type, 'ACTIVE'::card_status, 12500.25),
       ('4666666666666666', 6, DATE '2026-11-30','DEBIT'::card_type, 'BLOCKED'::card_status, 3000.00);

INSERT INTO transactions (from_card_id, to_card_id, amount, description, status, processed_at)
VALUES (1, 3, 1000.00, 'Перевод другу', 'SUCCESS'::transaction_status, NOW() - INTERVAL '2 days'),
       (3, 1, 500.00, 'Возврат долга', 'SUCCESS'::transaction_status, NOW() - INTERVAL '1 day'),
       (1, 5, 750.50, 'Оплата за услуги', 'SUCCESS'::transaction_status, NOW() - INTERVAL '3 hours'),
       (5, 1, 200.00, 'Перевод за покупки', 'SUCCESS'::transaction_status, NULL),
       (3, 6, 1500.00, 'Перевод на заблокированную карту', 'CANCELLED'::transaction_status, NOW() - INTERVAL '1 hour'),
       (1, 3, 100.00, 'Тестовый перевод', 'REFUNDED'::transaction_status, NOW() - INTERVAL '6 hours');

INSERT INTO card_applications (user_id, status, processed_at)
VALUES (2, 'APPROVED'::card_application_status, NOW() - INTERVAL '10 days'),
       (3, 'APPROVED'::card_application_status, NOW() - INTERVAL '7 days'),
       (4, 'APPROVED'::card_application_status, NOW() - INTERVAL '12 days'),
       (5, 'REJECTED'::card_application_status, NOW() - INTERVAL '3 days'),
       (6, 'APPROVED'::card_application_status, NOW() - INTERVAL '15 days'),
       (5, 'PENDING'::card_application_status, NULL),
       (6, 'CANCELLED'::card_application_status, NULL);

INSERT INTO refresh_sessions (user_id, refresh_token, fingerprint, ua, ip, expires_in)
VALUES (2, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid, 'fp_user2_device1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', '192.168.1.100',
        EXTRACT(EPOCH FROM (NOW() + INTERVAL '30 days'))::bigint * 1000),
       (2, 'b2c3d4e5-f6a7-8901-bcde-f23456789abc'::uuid, 'fp_user2_device2',
        'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)', '192.168.1.101',
        EXTRACT(EPOCH FROM (NOW() + INTERVAL '25 days'))::bigint * 1000),
       (3, 'c3d4e5f6-a7b8-9012-cdef-345678901def'::uuid, 'fp_user3_device1',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15', '10.0.0.15',
        EXTRACT(EPOCH FROM (NOW() + INTERVAL '28 days'))::bigint * 1000),
       (4, 'd4e5f6a7-b8c9-0123-defa-456789012fed'::uuid, 'fp_user4_device1',
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36', '172.16.0.50',
        EXTRACT(EPOCH FROM (NOW() + INTERVAL '20 days'))::bigint * 1000),
       (6, 'e5f6a7b8-c9d0-1234-efab-567890123bcd'::uuid, 'fp_user6_device1',
        'Mozilla/5.0 (Android 12; Mobile; rv:105.0) Gecko/105.0', '203.0.113.45',
        EXTRACT(EPOCH FROM (NOW() + INTERVAL '15 days'))::bigint * 1000)