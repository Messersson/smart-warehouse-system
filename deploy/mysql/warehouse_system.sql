CREATE DATABASE IF NOT EXISTS warehouse_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE warehouse_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS exception_ticket;
DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS approval_order;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS billing_statement_item;
DROP TABLE IF EXISTS billing_statement;
DROP TABLE IF EXISTS billing_rule;
DROP TABLE IF EXISTS billing_contract;
DROP TABLE IF EXISTS notification_message;
DROP TABLE IF EXISTS alert_event;
DROP TABLE IF EXISTS alert_rule;
DROP TABLE IF EXISTS warehouse_task;
DROP TABLE IF EXISTS stock_take_item;
DROP TABLE IF EXISTS stock_take_order;
DROP TABLE IF EXISTS inventory_movement;
DROP TABLE IF EXISTS inventory_stock;
DROP TABLE IF EXISTS outbound_order_item;
DROP TABLE IF EXISTS outbound_order;
DROP TABLE IF EXISTS inbound_order_item;
DROP TABLE IF EXISTS inbound_order;
DROP TABLE IF EXISTS base_product;
DROP TABLE IF EXISTS base_product_category;
DROP TABLE IF EXISTS base_carrier;
DROP TABLE IF EXISTS base_customer;
DROP TABLE IF EXISTS base_supplier;
DROP TABLE IF EXISTS base_owner;
DROP TABLE IF EXISTS base_location;
DROP TABLE IF EXISTS base_zone;
DROP TABLE IF EXISTS base_warehouse;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  role_code VARCHAR(64) NOT NULL DEFAULT 'ADMIN',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(128) NOT NULL,
  role_desc VARCHAR(255) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  menu_code VARCHAR(64) NOT NULL UNIQUE,
  menu_name VARCHAR(128) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  menu_path VARCHAR(255) DEFAULT NULL,
  component_name VARCHAR(128) DEFAULT NULL,
  icon_name VARCHAR(64) DEFAULT NULL,
  menu_type VARCHAR(32) NOT NULL DEFAULT 'MENU',
  permission_code VARCHAR(128) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  visible TINYINT(1) NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  module_name VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT DEFAULT NULL,
  action_type VARCHAR(64) NOT NULL,
  operator_name VARCHAR(128) DEFAULT NULL,
  request_ip VARCHAR(64) DEFAULT NULL,
  change_snapshot TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_warehouse (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(64) NOT NULL UNIQUE,
  warehouse_name VARCHAR(128) NOT NULL,
  warehouse_type VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  province VARCHAR(64) DEFAULT NULL,
  city VARCHAR(64) DEFAULT NULL,
  district VARCHAR(64) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  scene_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL_STORAGE',
  dwell_alert_minutes INT NOT NULL DEFAULT 0,
  sms_notify_enabled TINYINT(1) NOT NULL DEFAULT 0,
  sms_reminder_interval_minutes INT NOT NULL DEFAULT 120,
  auto_assign_location TINYINT(1) NOT NULL DEFAULT 1,
  scan_mode VARCHAR(32) NOT NULL DEFAULT 'QR_CODE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_zone (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  zone_code VARCHAR(64) NOT NULL,
  zone_name VARCHAR(128) NOT NULL,
  temperature_type VARCHAR(32) DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_zone_code (warehouse_id, zone_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_location (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  zone_name VARCHAR(128) DEFAULT NULL,
  location_code VARCHAR(64) NOT NULL,
  location_name VARCHAR(128) NOT NULL,
  aisle_no VARCHAR(32) DEFAULT NULL,
  shelf_no VARCHAR(32) DEFAULT NULL,
  layer_no VARCHAR(32) DEFAULT NULL,
  bin_no VARCHAR(32) DEFAULT NULL,
  capacity_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  used_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  pickable TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_location_code (warehouse_id, location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_owner (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_code VARCHAR(64) NOT NULL UNIQUE,
  owner_name VARCHAR(128) NOT NULL,
  owner_type VARCHAR(64) NOT NULL DEFAULT 'SELF',
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_supplier (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  supplier_code VARCHAR(64) NOT NULL UNIQUE,
  supplier_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_customer (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_code VARCHAR(64) NOT NULL UNIQUE,
  customer_name VARCHAR(128) NOT NULL,
  customer_type VARCHAR(64) NOT NULL DEFAULT 'B2B',
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_carrier (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  carrier_code VARCHAR(64) NOT NULL UNIQUE,
  carrier_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  service_level VARCHAR(64) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_product_category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  category_code VARCHAR(64) NOT NULL UNIQUE,
  category_name VARCHAR(128) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_product (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sku_code VARCHAR(64) NOT NULL UNIQUE,
  product_name VARCHAR(128) NOT NULL,
  product_spec VARCHAR(128) DEFAULT NULL,
  category_name VARCHAR(128) DEFAULT NULL,
  brand_name VARCHAR(128) DEFAULT NULL,
  unit_name VARCHAR(32) NOT NULL DEFAULT '件',
  barcode VARCHAR(64) DEFAULT NULL,
  safe_stock DECIMAL(18,2) NOT NULL DEFAULT 0,
  max_stock DECIMAL(18,2) NOT NULL DEFAULT 0,
  shelf_life_days INT NOT NULL DEFAULT 0,
  enable_batch TINYINT(1) NOT NULL DEFAULT 1,
  enable_serial TINYINT(1) NOT NULL DEFAULT 0,
  weight_kg DECIMAL(18,3) NOT NULL DEFAULT 0,
  volume_m3 DECIMAL(18,4) NOT NULL DEFAULT 0,
  sale_price DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inbound_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  supplier_id BIGINT DEFAULT NULL,
  owner_id BIGINT DEFAULT NULL,
  customer_id BIGINT DEFAULT NULL,
  order_type VARCHAR(64) NOT NULL DEFAULT 'PURCHASE',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  source_no VARCHAR(64) DEFAULT NULL,
  expected_arrival_time DATETIME DEFAULT NULL,
  actual_arrival_time DATETIME DEFAULT NULL,
  received_at DATETIME DEFAULT NULL,
  putaway_completed_at DATETIME DEFAULT NULL,
  total_expected_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_actual_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  operator_name VARCHAR(128) DEFAULT NULL,
  receiver_name VARCHAR(128) DEFAULT NULL,
  receiver_phone VARCHAR(32) DEFAULT NULL,
  pickup_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
  pickup_code VARCHAR(64) DEFAULT NULL,
  pickup_due_at DATETIME DEFAULT NULL,
  last_sms_notice_at DATETIME DEFAULT NULL,
  scan_code VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_inbound_status (status),
  KEY idx_inbound_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inbound_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  batch_no VARCHAR(64) DEFAULT NULL,
  production_date DATE DEFAULT NULL,
  expiry_date DATE DEFAULT NULL,
  expected_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  actual_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  qualified_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  location_id BIGINT DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_inbound_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  customer_id BIGINT DEFAULT NULL,
  owner_id BIGINT DEFAULT NULL,
  carrier_id BIGINT DEFAULT NULL,
  order_type VARCHAR(64) NOT NULL DEFAULT 'SALES',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  source_no VARCHAR(64) DEFAULT NULL,
  priority_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  planned_ship_time DATETIME DEFAULT NULL,
  picking_completed_at DATETIME DEFAULT NULL,
  packed_at DATETIME DEFAULT NULL,
  shipped_at DATETIME DEFAULT NULL,
  total_planned_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_shipped_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  operator_name VARCHAR(128) DEFAULT NULL,
  logistics_no VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_outbound_status (status),
  KEY idx_outbound_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  batch_no VARCHAR(64) DEFAULT NULL,
  planned_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  shipped_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  location_id BIGINT DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_outbound_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_stock (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  owner_id BIGINT DEFAULT NULL,
  product_id BIGINT NOT NULL,
  location_id BIGINT DEFAULT NULL,
  batch_no VARCHAR(64) DEFAULT NULL,
  quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
  locked_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  available_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  last_inbound_at DATETIME DEFAULT NULL,
  last_outbound_at DATETIME DEFAULT NULL,
  last_movement_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_stock_product (product_id),
  KEY idx_stock_warehouse (warehouse_id),
  KEY idx_stock_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_movement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  owner_id BIGINT DEFAULT NULL,
  product_id BIGINT NOT NULL,
  location_id BIGINT DEFAULT NULL,
  batch_no VARCHAR(64) DEFAULT NULL,
  movement_type VARCHAR(64) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_id BIGINT DEFAULT NULL,
  source_no VARCHAR(64) DEFAULT NULL,
  before_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  change_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  after_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  operator_name VARCHAR(128) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_inventory_movement_product (product_id),
  KEY idx_inventory_movement_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_take_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  take_no VARCHAR(64) NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  owner_id BIGINT DEFAULT NULL,
  take_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  planned_start_time DATETIME DEFAULT NULL,
  planned_end_time DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  operator_name VARCHAR(128) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_take_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  take_order_id BIGINT NOT NULL,
  stock_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  system_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  actual_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  diff_qty DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE warehouse_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_no VARCHAR(64) NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  priority_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  assignee_name VARCHAR(128) DEFAULT NULL,
  assigned_at DATETIME DEFAULT NULL,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  due_time DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  approval_no VARCHAR(64) NOT NULL UNIQUE,
  approval_type VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  biz_no VARCHAR(64) DEFAULT NULL,
  applicant_name VARCHAR(128) DEFAULT NULL,
  approver_name VARCHAR(128) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  current_node VARCHAR(64) DEFAULT NULL,
  apply_reason VARCHAR(255) DEFAULT NULL,
  approval_comment VARCHAR(255) DEFAULT NULL,
  applied_at DATETIME DEFAULT NULL,
  decided_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_approval_order_status (status),
  KEY idx_approval_order_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  approval_order_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  operator_name VARCHAR(128) DEFAULT NULL,
  action_comment VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exception_ticket (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ticket_no VARCHAR(64) NOT NULL UNIQUE,
  warehouse_id BIGINT DEFAULT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT DEFAULT NULL,
  biz_no VARCHAR(64) DEFAULT NULL,
  ticket_title VARCHAR(128) NOT NULL,
  ticket_content TEXT,
  severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  assignee_name VARCHAR(128) DEFAULT NULL,
  reporter_name VARCHAR(128) DEFAULT NULL,
  approval_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
  reported_at DATETIME DEFAULT NULL,
  resolved_at DATETIME DEFAULT NULL,
  closed_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_exception_ticket_status (status),
  KEY idx_exception_ticket_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alert_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  rule_code VARCHAR(64) NOT NULL UNIQUE,
  rule_name VARCHAR(128) NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  threshold_value INT NOT NULL DEFAULT 0,
  threshold_unit VARCHAR(32) NOT NULL DEFAULT 'MINUTE',
  severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  notify_to VARCHAR(255) DEFAULT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alert_event (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  alert_code VARCHAR(128) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT DEFAULT NULL,
  severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  alert_message VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  first_triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  acknowledged_by VARCHAR(128) DEFAULT NULL,
  acknowledged_at DATETIME DEFAULT NULL,
  resolved_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_alert_event_status (status),
  KEY idx_alert_event_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  channel_type VARCHAR(32) NOT NULL,
  receiver_name VARCHAR(128) NOT NULL,
  receiver_phone VARCHAR(32) DEFAULT NULL,
  warehouse_id BIGINT DEFAULT NULL,
  biz_type VARCHAR(64) DEFAULT NULL,
  biz_id BIGINT DEFAULT NULL,
  message_title VARCHAR(128) NOT NULL,
  message_body TEXT NOT NULL,
  send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  response_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  last_error TEXT DEFAULT NULL,
  sent_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_contract (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  owner_id BIGINT DEFAULT NULL,
  warehouse_id BIGINT DEFAULT NULL,
  contract_no VARCHAR(64) NOT NULL UNIQUE,
  contract_name VARCHAR(128) NOT NULL,
  effective_date DATE NOT NULL,
  expire_date DATE DEFAULT NULL,
  settlement_cycle VARCHAR(32) NOT NULL DEFAULT 'MONTHLY',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  charge_type VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  unit_name VARCHAR(32) NOT NULL,
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
  step_json TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_statement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  statement_no VARCHAR(64) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  statement_month VARCHAR(16) NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  statement_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_statement_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  statement_id BIGINT NOT NULL,
  charge_type VARCHAR(64) NOT NULL,
  charge_name VARCHAR(128) NOT NULL,
  quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
  unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
  amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  biz_no VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (role_code, role_name, role_desc) VALUES
('ADMIN', '系统管理员', '拥有全部权限'),
('MANAGER', '仓库主管', '负责仓库管理与异常处理'),
('OPERATOR', '仓库作业员', '负责日常作业');

INSERT INTO sys_user (username, password, display_name, phone, email, role_code, status, remark) VALUES
('admin', 'admin123', '系统管理员', '13800000000', 'admin@example.com', 'ADMIN', 'ACTIVE', '初始化管理员账号');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_path, component_name, icon_name, menu_type, permission_code, sort_no, visible, status) VALUES
('DASHBOARD', 'Dashboard', NULL, '/dashboard', 'Dashboard', 'DataBoard', 'MENU', 'dashboard:view', 1, 1, 'ACTIVE'),
('MASTER_WAREHOUSES', 'Warehouses', NULL, '/master/warehouses', 'ResourcePage', 'OfficeBuilding', 'MENU', 'warehouse:view', 10, 1, 'ACTIVE'),
('MASTER_LOCATIONS', 'Locations', NULL, '/master/locations', 'ResourcePage', 'Grid', 'MENU', 'location:view', 11, 1, 'ACTIVE'),
('MASTER_OWNERS', 'Owners', NULL, '/master/owners', 'ResourcePage', 'User', 'MENU', 'owner:view', 12, 1, 'ACTIVE'),
('MASTER_SUPPLIERS', 'Suppliers', NULL, '/master/suppliers', 'ResourcePage', 'Van', 'MENU', 'supplier:view', 13, 1, 'ACTIVE'),
('MASTER_CUSTOMERS', 'Customers', NULL, '/master/customers', 'ResourcePage', 'UserFilled', 'MENU', 'customer:view', 14, 1, 'ACTIVE'),
('MASTER_PRODUCTS', 'Products', NULL, '/master/products', 'ResourcePage', 'Goods', 'MENU', 'product:view', 15, 1, 'ACTIVE'),
('INBOUNDS', 'Inbounds', NULL, '/inbounds', 'Inbounds', 'Download', 'MENU', 'inbound:view', 20, 1, 'ACTIVE'),
('OUTBOUNDS', 'Outbounds', NULL, '/outbounds', 'Outbounds', 'Upload', 'MENU', 'outbound:view', 21, 1, 'ACTIVE'),
('STOCKS', 'Stocks', NULL, '/stocks', 'Stocks', 'Box', 'MENU', 'stock:view', 22, 1, 'ACTIVE'),
('ALERTS', 'Alerts', NULL, '/alerts', 'Alerts', 'Bell', 'MENU', 'alert:view', 23, 1, 'ACTIVE'),
('PERMISSIONS', 'Permissions', NULL, '/permissions', 'Permissions', 'Lock', 'MENU', 'permission:view', 24, 1, 'ACTIVE'),
('STOCK_TAKES', 'Stock Takes', NULL, '/stock-takes', 'StockTakes', 'List', 'MENU', 'stocktake:view', 25, 1, 'ACTIVE'),
('BILLING', 'Billing', NULL, '/billing', 'Billing', 'Money', 'MENU', 'billing:view', 26, 1, 'ACTIVE'),
('APPROVALS', 'Approvals', NULL, '/approvals', 'Approvals', 'Select', 'MENU', 'approval:view', 27, 1, 'ACTIVE'),
('EXCEPTIONS', 'Exceptions', NULL, '/exceptions', 'Exceptions', 'Warning', 'MENU', 'exception:view', 28, 1, 'ACTIVE');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE menu_code IN (
  'DASHBOARD', 'MASTER_WAREHOUSES', 'MASTER_LOCATIONS', 'MASTER_OWNERS', 'MASTER_SUPPLIERS',
  'MASTER_CUSTOMERS', 'MASTER_PRODUCTS', 'INBOUNDS', 'OUTBOUNDS', 'STOCKS', 'ALERTS',
  'STOCK_TAKES', 'BILLING', 'APPROVALS', 'EXCEPTIONS'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE menu_code IN (
  'DASHBOARD', 'INBOUNDS', 'OUTBOUNDS', 'STOCKS', 'ALERTS', 'STOCK_TAKES', 'EXCEPTIONS'
);

INSERT INTO base_warehouse (warehouse_code, warehouse_name, warehouse_type, contact_name, contact_phone, province, city, district, address, status, remark) VALUES
('WH-SH-001', '上海中心仓', 'GENERAL', '张主管', '13900000001', '上海', '上海', '浦东新区', '临港新片区仓储大道100号', 'ACTIVE', '默认示例仓库');

INSERT INTO base_zone (warehouse_id, zone_code, zone_name, temperature_type, status) VALUES
(1, 'Z-RECV', '收货区', 'NORMAL', 'ACTIVE'),
(1, 'Z-PICK', '拣选区', 'NORMAL', 'ACTIVE'),
(1, 'Z-COLD', '冷藏区', 'COLD', 'ACTIVE');

INSERT INTO base_location (warehouse_id, zone_name, location_code, location_name, aisle_no, shelf_no, layer_no, bin_no, capacity_qty, used_qty, status, pickable, remark) VALUES
(1, '收货区', 'A-01-01-01', 'A排1架1层1位', 'A', '01', '01', '01', 1000, 0, 'ACTIVE', 1, '默认库位'),
(1, '拣选区', 'B-01-01-01', 'B排1架1层1位', 'B', '01', '01', '01', 1000, 0, 'ACTIVE', 1, '默认拣选位'),
(1, '冷藏区', 'C-01-01-01', 'C排1架1层1位', 'C', '01', '01', '01', 500, 0, 'ACTIVE', 1, '默认冷藏位');

INSERT INTO base_owner (owner_code, owner_name, owner_type, contact_name, contact_phone, email, address, status, remark) VALUES
('OWN-001', '自营货主', 'SELF', '李经理', '13900000002', 'owner@example.com', '上海浦东新区', 'ACTIVE', '默认货主');

INSERT INTO base_supplier (supplier_code, supplier_name, contact_name, contact_phone, email, address, status, remark) VALUES
('SUP-001', '华东供应商', '王采购', '13900000003', 'supplier@example.com', '江苏昆山', 'ACTIVE', '默认供应商');

INSERT INTO base_customer (customer_code, customer_name, customer_type, contact_name, contact_phone, email, address, status, remark) VALUES
('CUS-001', '重点客户A', 'B2B', '赵经理', '13900000004', 'customer@example.com', '浙江杭州', 'ACTIVE', '默认客户');

INSERT INTO base_carrier (carrier_code, carrier_name, contact_name, contact_phone, service_level, status, remark) VALUES
('CAR-001', '顺达物流', '孙调度', '13900000005', '次日达', 'ACTIVE', '默认承运商');

INSERT INTO base_product_category (category_code, category_name, parent_id, status) VALUES
('CAT-001', '日用品', NULL, 'ACTIVE'),
('CAT-002', '冷链食品', NULL, 'ACTIVE');

INSERT INTO base_product (sku_code, product_name, product_spec, category_name, brand_name, unit_name, barcode, safe_stock, max_stock, shelf_life_days, enable_batch, enable_serial, weight_kg, volume_m3, sale_price, status, remark) VALUES
('SKU-001', '矿泉水 550ml', '24瓶/箱', '日用品', '清泉', '箱', '690000000001', 20, 5000, 365, 1, 0, 12.000, 0.0450, 36.00, 'ACTIVE', '默认商品'),
('SKU-002', '冷藏牛奶 250ml', '12盒/箱', '冷链食品', '牧场', '箱', '690000000002', 10, 2000, 30, 1, 0, 6.000, 0.0220, 58.00, 'ACTIVE', '默认商品');

INSERT INTO alert_rule (rule_code, rule_name, rule_type, biz_type, threshold_value, threshold_unit, severity, notify_to, enabled, remark) VALUES
('INBOUND_RECEIVE_TIMEOUT', '收货超时预警', 'TIMEOUT', 'INBOUND_ORDER', 120, 'MINUTE', 'MEDIUM', '仓库主管', 1, '预计到仓后超过120分钟未收货'),
('INBOUND_PUTAWAY_TIMEOUT', '上架滞留预警', 'TIMEOUT', 'INBOUND_ORDER', 120, 'MINUTE', 'HIGH', '仓库主管,仓库经理', 1, '收货后超过120分钟未上架'),
('OUTBOUND_SHIP_TIMEOUT', '出库超时预警', 'TIMEOUT', 'OUTBOUND_ORDER', 60, 'MINUTE', 'HIGH', '仓库主管,客服经理', 1, '计划发货时间后60分钟未发出'),
('STOCK_STAGNANT', '呆滞库存预警', 'AGING', 'INVENTORY_STOCK', 30, 'DAY', 'MEDIUM', '仓库主管,销售经理', 1, '库存超过30天无变动');

INSERT INTO inbound_order (order_no, warehouse_id, supplier_id, owner_id, order_type, status, source_no, expected_arrival_time, actual_arrival_time, received_at, total_expected_qty, total_actual_qty, operator_name, remark) VALUES
('IN202604150001', 1, 1, 1, 'PURCHASE', 'RECEIVED', 'PO202604150001', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), 100, 100, '系统管理员', '示例入库单，故意保留未上架用于演示滞留预警');

INSERT INTO inbound_order_item (order_id, product_id, sku_code, product_name, batch_no, production_date, expiry_date, expected_qty, actual_qty, qualified_qty, location_id, remark) VALUES
(1, 1, 'SKU-001', '矿泉水 550ml', 'BATCH-20260415-001', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 100, 100, 100, 1, '默认示例明细');

INSERT INTO outbound_order (order_no, warehouse_id, customer_id, owner_id, carrier_id, order_type, status, source_no, priority_level, planned_ship_time, total_planned_qty, total_shipped_qty, operator_name, remark) VALUES
('OUT202604150001', 1, 1, 1, 1, 'SALES', 'CREATED', 'SO202604150001', 'HIGH', DATE_SUB(NOW(), INTERVAL 2 HOUR), 20, 0, '系统管理员', '示例出库单，用于演示出库超时预警');

INSERT INTO outbound_order_item (order_id, product_id, sku_code, product_name, batch_no, planned_qty, shipped_qty, location_id, remark) VALUES
(1, 1, 'SKU-001', '矿泉水 550ml', 'BATCH-20260415-001', 20, 0, 2, '默认示例明细');

INSERT INTO inventory_stock (warehouse_id, owner_id, product_id, location_id, batch_no, quantity, locked_qty, available_qty, last_inbound_at, last_outbound_at, last_movement_at) VALUES
(1, 1, 1, 2, 'BATCH-20260301-001', 300, 20, 280, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY)),
(1, 1, 2, 3, 'BATCH-20260401-002', 80, 0, 80, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT INTO inventory_movement (warehouse_id, owner_id, product_id, location_id, batch_no, movement_type, source_type, source_id, source_no, before_qty, change_qty, after_qty, operator_name, remark) VALUES
(1, 1, 1, 2, 'BATCH-20260301-001', 'INBOUND', 'INBOUND_ORDER', 1, 'IN202604150001', 200, 100, 300, '系统管理员', '初始化库存流水');

INSERT INTO stock_take_order (take_no, warehouse_id, owner_id, take_type, status, planned_start_time, planned_end_time, operator_name, remark) VALUES
('TAKE202604150001', 1, 1, 'CYCLE', 'COUNTING', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), '系统管理员', '默认盘点任务');

INSERT INTO stock_take_item (take_order_id, stock_id, product_id, system_qty, actual_qty, diff_qty, status, remark) VALUES
(1, 1, 1, 300, 296, -4, 'DIFF', '盘亏示例'),
(1, 2, 2, 80, 80, 0, 'MATCHED', '盘点正常');

INSERT INTO billing_contract (customer_id, owner_id, warehouse_id, contract_no, contract_name, effective_date, expire_date, settlement_cycle, status) VALUES
(1, 1, 1, 'BC202604150001', '重点客户A标准合同', '2026-01-01', '2026-12-31', 'MONTHLY', 'ACTIVE');

INSERT INTO billing_rule (contract_id, charge_type, rule_name, unit_name, unit_price, step_json, status) VALUES
(1, 'OUTBOUND_ORDER_COUNT', '出库单服务费', '单', 15.00, NULL, 'ACTIVE'),
(1, 'OUTBOUND_QTY', '出库件数服务费', '件', 0.80, NULL, 'ACTIVE'),
(1, 'INBOUND_QTY', '入库件数服务费', '件', 0.60, NULL, 'ACTIVE'),
(1, 'STORAGE_SNAPSHOT_QTY', '库存快照占用费', '件', 0.10, NULL, 'ACTIVE');

INSERT INTO approval_order (approval_no, approval_type, biz_type, biz_id, biz_no, applicant_name, approver_name, status, current_node, apply_reason, approval_comment, applied_at, decided_at) VALUES
('AP202604150001', 'ORDER_APPROVAL', 'OUTBOUND_ORDER', 1, 'OUT202604150001', '系统管理员', '仓库主管', 'PENDING', 'MANAGER_REVIEW', '加急订单需要主管确认', NULL, NOW(), NULL);

INSERT INTO approval_record (approval_order_id, action_type, operator_name, action_comment) VALUES
(1, 'SUBMIT', '系统管理员', '提交出库单审批');

INSERT INTO exception_ticket (ticket_no, warehouse_id, biz_type, biz_id, biz_no, ticket_title, ticket_content, severity, status, assignee_name, reporter_name, approval_status, reported_at) VALUES
('EX202604150001', 1, 'INBOUND_ORDER', 1, 'IN202604150001', 'Inbound Putaway Delay', 'The inbound order has stayed too long after receiving and still waits for putaway.', 'HIGH', 'OPEN', '仓库主管', '系统管理员', 'NOT_SUBMITTED', NOW()),
('EX202604150002', 1, 'STOCK_TAKE_ORDER', 1, 'TAKE202604150001', 'Stock Take Difference', 'Cycle count found a quantity gap that requires review.', 'MEDIUM', 'IN_PROGRESS', '库存专员', '系统管理员', 'PENDING', NOW());

SET FOREIGN_KEY_CHECKS = 1;
