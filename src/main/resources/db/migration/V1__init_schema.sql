create table app_user (
    id            uuid primary key,
    email         varchar(254) not null unique,
    password_hash varchar(255) not null,
    role          varchar(16)  not null default 'USER',
    active        boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table refresh_token (
    id          uuid primary key,
    user_id     uuid not null references app_user(id) on delete cascade,
    token_hash  varchar(255) not null unique,
    expires_at  timestamptz  not null,
    created_at  timestamptz  not null default now()
);
create index ix_refresh_token_user on refresh_token(user_id);

create table task (
    id          uuid primary key,
    owner_id    uuid not null references app_user(id) on delete cascade,
    title       varchar(200) not null,
    description text,
    status      varchar(20)  not null default 'TODO',
    priority    int          not null default 3,
    due_date    date,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);
create index ix_task_owner_status on task(owner_id, status);

create table tag (
    id   uuid primary key,
    name varchar(50) not null unique
);

create table task_tag (
    task_id uuid not null references task(id) on delete cascade,
    tag_id  uuid not null references tag(id)  on delete cascade,
    primary key (task_id, tag_id)
);
