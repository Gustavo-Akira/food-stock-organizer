-- Users (auth domain)
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Houses (household domain)
CREATE TABLE houses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    owner_id    UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- House members (household domain)
CREATE TABLE house_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    house_id    UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    role        VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (house_id, user_id)
);

-- Inventory items (inventory domain)
CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    house_id        UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    quantity_level  VARCHAR(50) NOT NULL DEFAULT 'ENOUGH',
    expiry_date     DATE,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Shopping lists (shopping domain)
CREATE TABLE shopping_lists (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    house_id    UUID NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Shopping list items (shopping domain)
CREATE TABLE shopping_list_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    inventory_item_id UUID REFERENCES inventory_items(id),
    name             VARCHAR(255) NOT NULL,
    quantity         INTEGER NOT NULL DEFAULT 1,
    checked          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_house_members_house_id ON house_members(house_id);
CREATE INDEX idx_house_members_user_id ON house_members(user_id);
CREATE INDEX idx_inventory_items_house_id ON inventory_items(house_id);
CREATE INDEX idx_inventory_items_quantity_level ON inventory_items(quantity_level);
CREATE INDEX idx_shopping_lists_house_id ON shopping_lists(house_id);
CREATE INDEX idx_shopping_list_items_list_id ON shopping_list_items(shopping_list_id);
