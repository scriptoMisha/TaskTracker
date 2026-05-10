-- Seed an admin user for local development.
-- Email:    admin@minitasks.local
-- Password: password   (BCrypt cost 10; well-known dev hash. DO NOT use in prod.)
insert into app_user (id, email, password_hash, role, active)
values (
    '00000000-0000-0000-0000-000000000001',
    'admin@minitasks.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    true
)
on conflict (email) do nothing;
