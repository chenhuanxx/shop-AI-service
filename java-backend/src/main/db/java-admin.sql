/*
 Navicat Premium Dump SQL

 Source Server         : java-admin
 Source Server Type    : MySQL
 Source Server Version : 80024 (8.0.24)
 Source Host           : 223.109.140.116:3306
 Source Schema         : java-admin

 Target Server Type    : MySQL
 Target Server Version : 80024 (8.0.24)
 File Encoding         : 65001

 Date: 29/04/2026 17:04:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for approvals
-- ----------------------------
DROP TABLE IF EXISTS `approvals`;
CREATE TABLE `approvals`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `state` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at_str` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `apply_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `office_opinion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `department_opinion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `course_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_approvals_course_id`(`course_id` ASC) USING BTREE,
  INDEX `ix_approvals_title`(`title` ASC) USING BTREE,
  INDEX `ix_approvals_created_at_str`(`created_at_str` ASC) USING BTREE,
  INDEX `ix_approvals_id`(`id` ASC) USING BTREE,
  INDEX `ix_approvals_user_id`(`user_id` ASC) USING BTREE,
  INDEX `ix_approvals_state`(`state` ASC) USING BTREE,
  CONSTRAINT `approvals_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `booking_courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `approvals_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of approvals
-- ----------------------------
INSERT INTO `approvals` VALUES ('a_07fe286e', '课程预约', '已取消', '2026-04-09 11:08', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-09 03:08:57');
INSERT INTO `approvals` VALUES ('a_0fd447a4', '课程预约', '已取消', '2026-03-26 22:25', '取消预约', '', '', 'pca_4f4a3f9dde', 2, '2026-03-26 14:25:46');
INSERT INTO `approvals` VALUES ('a_117fa23b', '课程预约', '已预约', '2026-04-17 11:25', '申请预约该课程', '', '', 'pca_6a10cc7f50', 1, '2026-04-17 03:25:04');
INSERT INTO `approvals` VALUES ('a_1e59c36c', '课程预约', '已预约', '2026-04-21 09:12', '申请预约该课程', '', '', 'pca_50e58e5fdd', 3, '2026-04-21 01:12:41');
INSERT INTO `approvals` VALUES ('a_25fc6b81', '课程预约', '已取消', '2026-04-17 15:36', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 07:36:34');
INSERT INTO `approvals` VALUES ('a_33a72582', '课程预约', '已取消', '2026-04-17 14:15', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 06:15:41');
INSERT INTO `approvals` VALUES ('a_358957fd', '课程预约', '已预约', '2026-03-26 21:05', '申请预约该课程', '', '', 'pca_56b47b5421', 1, '2026-03-26 13:05:37');
INSERT INTO `approvals` VALUES ('a_3d0fe214', '课程预约', '已取消', '2026-03-27 09:27', '取消预约', '', '', 'pca_4f4a3f9dde', 1, '2026-03-27 01:27:45');
INSERT INTO `approvals` VALUES ('a_42107e4f', '课程预约', '已取消', '2026-04-17 16:07', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 08:07:13');
INSERT INTO `approvals` VALUES ('a_42dcdc94', '课程预约', '已取消', '2026-03-26 21:07', '取消预约', '', '', 'pca_56b47b5421', 1, '2026-03-26 13:07:23');
INSERT INTO `approvals` VALUES ('a_5609ee20', '课程预约', '已取消', '2026-04-17 15:36', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 07:36:20');
INSERT INTO `approvals` VALUES ('a_5738c522', '课程预约', '已取消', '2026-04-17 15:46', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 07:46:04');
INSERT INTO `approvals` VALUES ('a_57cde050', '课程预约', '已取消', '2026-04-17 15:51', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 07:51:33');
INSERT INTO `approvals` VALUES ('a_5c62979e', '课程预约', '已取消', '2026-04-17 15:59', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 07:59:45');
INSERT INTO `approvals` VALUES ('a_6a7844cd', '课程预约', '已预约', '2026-03-26 22:25', '申请预约该课程', '', '', 'pca_4f4a3f9dde', 2, '2026-03-26 14:25:43');
INSERT INTO `approvals` VALUES ('a_6fd3e6e1', '课程预约', '已预约', '2026-04-17 16:19', '申请预约该课程', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 08:19:04');
INSERT INTO `approvals` VALUES ('a_774e77bb', '课程预约', '已预约', '2026-04-17 16:07', '申请预约该课程', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 08:07:37');
INSERT INTO `approvals` VALUES ('a_91da790c', '课程预约', '已取消', '2026-04-17 15:45', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 07:45:57');
INSERT INTO `approvals` VALUES ('a_b1e0dd14', '课程预约', '已取消', '2026-04-17 15:50', '取消预约', '', '', 'pca_6a10cc7f50', 2, '2026-04-17 07:50:58');
INSERT INTO `approvals` VALUES ('a_b46e3590', '课程预约', '已预约', '2026-03-26 22:16', '申请预约该课程', '', '', 'pca_4f4a3f9dde', 1, '2026-03-26 14:16:47');
INSERT INTO `approvals` VALUES ('a_cfcda607', '课程预约', '已取消', '2026-04-17 16:00', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 08:00:37');
INSERT INTO `approvals` VALUES ('a_dc76eb99', '课程预约', '已取消', '2026-03-26 22:02', '取消预约', '', '', 'pca_56b47b5421', 1, '2026-03-26 14:02:58');
INSERT INTO `approvals` VALUES ('a_e7cbd33f', '课程预约', '已取消', '2026-04-17 15:45', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 07:45:04');
INSERT INTO `approvals` VALUES ('a_f2b1d84b', '课程预约', '已取消', '2026-04-17 15:50', '取消预约', '', '', 'pca_e74a9fbb79', 2, '2026-04-17 07:50:43');
INSERT INTO `approvals` VALUES ('a_fdc3a3b9', '课程预约', '已预约', '2026-03-26 21:20', '申请预约该课程', '', '', 'pca_56b47b5421', 1, '2026-03-26 13:20:15');

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `link_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT 0,
  `enabled` tinyint(1) NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of banner
-- ----------------------------
INSERT INTO `banner` VALUES (1, '测试banner', '/uploads/33135ac7-7d8f-4b0c-aa93-da03b350bd47.jpg', '', 0, 1, '2026-04-17 02:58:14', '2026-04-17 09:22:10');
INSERT INTO `banner` VALUES (2, '21212', '/uploads/c9b099a5-4c0a-47b6-8a9b-a00d907bb928.jpg', '', 1, 1, '2026-04-17 03:05:10', '2026-04-17 09:22:18');
INSERT INTO `banner` VALUES (3, '2dsad', '/uploads/aa8f2d59-ef53-4eb0-a737-3df092a39ac1.jpg', '', 1, 1, '2026-04-17 03:05:27', '2026-04-17 09:22:32');

-- ----------------------------
-- Table structure for booking_courses
-- ----------------------------
DROP TABLE IF EXISTS `booking_courses`;
CREATE TABLE `booking_courses`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `start_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `teacher_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enrolled_count` int NOT NULL,
  `capacity` int NOT NULL,
  `academic_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `term` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `office` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `course` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `topic` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_booking_courses_start_at`(`start_at` ASC) USING BTREE,
  INDEX `ix_booking_courses_teacher_name`(`teacher_name` ASC) USING BTREE,
  INDEX `ix_booking_courses_id`(`id` ASC) USING BTREE,
  INDEX `ix_booking_courses_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of booking_courses
-- ----------------------------
INSERT INTO `booking_courses` VALUES ('pca_4f4a3f9dde', '课程名称1', '2026-03-31 08:00', 'admin', 0, 50, '2025-2026', '第一学期', '教育学院', '2024 级 教育学 1 班', '11', '12', '教学观摩', '课题', '2026-03-26 13:08:55');
INSERT INTO `booking_courses` VALUES ('pca_50e58e5fdd', '课程名称教研室11', '2026-03-10 08:00', 'user', 0, 50, '2025-2026', '第一学期', '教育学院', '2024 级 教育学 2 班', '教研室', '教研室', '示范课', '课题-称教研室11', '2026-03-26 14:19:48');
INSERT INTO `booking_courses` VALUES ('pca_56b47b5421', '2', '2026-03-27 08:00', '1', 0, 50, '2025-2026', '第一学期', '信息工程学院', '2023 级 计算机 2 班', '12', '12', '示范课', '1212222', '2026-03-26 05:17:25');
INSERT INTO `booking_courses` VALUES ('pca_6a10cc7f50', '课程名称呃呃呃呃', '2026-04-30 08:00', 'user', 1, 50, '2025-2026', '第一学期', '信息工程学院', '2023 级 计算机 1 班', '教研室', '开课地点', '示范课', '课题1', '2026-04-09 03:02:05');
INSERT INTO `booking_courses` VALUES ('pca_dfb1d3fe85', '伟大我去', '2026-04-02 08:00', 'user', 0, 50, '2025-2026', '第一学期', '信息工程学院', '2024 级 软件工程 1 班', '伟大我去', '伟大我去', '教学观摩', '伟大我去', '2026-03-27 01:18:56');
INSERT INTO `booking_courses` VALUES ('pca_e74a9fbb79', '出现在曹张新村', '2026-04-30 14:00', 'user', 1, 50, '2025-2026', '第一学期', '外国语学院', '2024 级 英语 1 班', '出现在曹张新村', '出现在曹张新村', '教学观摩', '出现在曹张新村', '2026-04-17 06:48:51');
INSERT INTO `booking_courses` VALUES ('pca_fa92928f58', '课程名称-0教研室12212', '2026-03-31 08:00', 'admin', 0, 50, '2025-2026', '第一学期', '外国语学院', '2024 级 英语 1 班', '教研室12', '开课地点-0教研室12212', '研讨课', '课题-0教研室12212', '2026-03-26 14:22:10');

-- ----------------------------
-- Table structure for booking_reservations
-- ----------------------------
DROP TABLE IF EXISTS `booking_reservations`;
CREATE TABLE `booking_reservations`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `qr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_booking_reservations_created_at`(`created_at` ASC) USING BTREE,
  INDEX `ix_booking_reservations_user_id`(`user_id` ASC) USING BTREE,
  INDEX `ix_booking_reservations_id`(`id` ASC) USING BTREE,
  CONSTRAINT `booking_reservations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of booking_reservations
-- ----------------------------

-- ----------------------------
-- Table structure for cart_items
-- ----------------------------
DROP TABLE IF EXISTS `cart_items`;
CREATE TABLE `cart_items`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `price` decimal(38, 2) NOT NULL,
  `product_id` int NOT NULL,
  `quantity` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK1re40cjegsfvw58xrkdp6bac6`(`product_id` ASC) USING BTREE,
  CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart_items
-- ----------------------------
INSERT INTO `cart_items` VALUES (1, '2026-04-16 08:58:53.150986', 187.00, 3, 1, '2026-04-16 08:58:53.150986', 1);
INSERT INTO `cart_items` VALUES (2, '2026-04-17 07:33:13.231107', 187.00, 3, 1, '2026-04-17 07:33:13.231107', 2);

-- ----------------------------
-- Table structure for course_checkins
-- ----------------------------
DROP TABLE IF EXISTS `course_checkins`;
CREATE TABLE `course_checkins`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_checkins
-- ----------------------------
INSERT INTO `course_checkins` VALUES (1, 'pca_4f4a3f9dde', '2026-03-26 14:35:06.338198', 2);
INSERT INTO `course_checkins` VALUES (2, 'pca_6a10cc7f50', '2026-04-17 03:31:50.851706', 1);

-- ----------------------------
-- Table structure for favorites
-- ----------------------------
DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `product_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK6sgu5npe8ug4o42bf9j71x20c`(`product_id` ASC) USING BTREE,
  CONSTRAINT `FK6sgu5npe8ug4o42bf9j71x20c` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorites
-- ----------------------------
INSERT INTO `favorites` VALUES (1, '2026-04-16 09:28:32.424070', 4, 1);
INSERT INTO `favorites` VALUES (2, '2026-04-17 07:33:07.409397', 3, 2);

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at_str` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `read` tinyint(1) NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_messages_id`(`id` ASC) USING BTREE,
  INDEX `ix_messages_user_id`(`user_id` ASC) USING BTREE,
  INDEX `ix_messages_created_at_str`(`created_at_str` ASC) USING BTREE,
  INDEX `ix_messages_title`(`title` ASC) USING BTREE,
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES ('m_05df1b28', '预约申请已提交', '你已提交《课程名称1》的预约申请。', '2026-04-17 11:25', 0, 1, '2026-04-17 03:25:04');
INSERT INTO `messages` VALUES ('m_1968ddf1', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 15:46', 0, 2, '2026-04-17 07:46:04');
INSERT INTO `messages` VALUES ('m_1ba1f5c9', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 14:15', 0, 2, '2026-04-17 06:15:41');
INSERT INTO `messages` VALUES ('m_2a73c061', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 16:06', 0, 2, '2026-04-17 08:06:46');
INSERT INTO `messages` VALUES ('m_31c377b4', '预约申请已提交', '你已提交《2》的预约申请。', '2026-03-26 21:20', 0, 1, '2026-03-26 13:20:15');
INSERT INTO `messages` VALUES ('m_344495e1', '已取消预约', '你已取消《课堂管理与教学设计》的预约。', '2026-03-25 23:46', 0, 1, '2026-03-25 15:46:25');
INSERT INTO `messages` VALUES ('m_3471f9e7', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 15:59', 0, 2, '2026-04-17 07:59:45');
INSERT INTO `messages` VALUES ('m_375cee25', '预约申请已提交', '你已提交《课程名称1》的预约申请。', '2026-03-26 22:16', 0, 1, '2026-03-26 14:16:47');
INSERT INTO `messages` VALUES ('m_3c3ded44', '已取消预约', '你已取消《12》的预约。', '2026-03-26 13:19', 0, 1, '2026-03-26 05:19:44');
INSERT INTO `messages` VALUES ('m_4decbdf4', '预约申请已提交', '你已提交《课堂管理与教学设计》的预约申请。', '2026-03-25 23:46', 0, 1, '2026-03-25 15:46:24');
INSERT INTO `messages` VALUES ('m_66e6ae01', '预约申请已提交', '你已提交《课程名称呃呃呃呃》的预约申请。', '2026-04-17 16:07', 0, 2, '2026-04-17 08:07:37');
INSERT INTO `messages` VALUES ('m_6dcb8097', '已取消预约', '你已取消《课程名称1》的预约。', '2026-03-26 22:25', 0, 2, '2026-03-26 14:25:46');
INSERT INTO `messages` VALUES ('m_7a038f07', '预约申请已提交', '你已提交《12》的预约申请。', '2026-03-26 21:05', 0, 1, '2026-03-26 13:05:37');
INSERT INTO `messages` VALUES ('m_80560f63', '预约申请已提交', '你已提交《课程名称1》的预约申请。', '2026-04-09 11:08', 0, 2, '2026-04-09 03:08:57');
INSERT INTO `messages` VALUES ('m_80566a61', '预约申请已提交', '你已提交《课堂管理与教学设计》的预约申请。', '2026-03-26 09:38', 0, 2, '2026-03-26 01:38:32');
INSERT INTO `messages` VALUES ('m_8240ccb8', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 16:06', 0, 2, '2026-04-17 08:06:51');
INSERT INTO `messages` VALUES ('m_84b48e70', '预约申请已提交', '你已提交《出现在曹张新村》的预约申请。', '2026-04-17 15:36', 0, 2, '2026-04-17 07:36:20');
INSERT INTO `messages` VALUES ('m_89f890c1', '预约申请已提交', '你已提交《课程名称呃呃呃呃》的预约申请。', '2026-04-17 16:07', 0, 2, '2026-04-17 08:07:13');
INSERT INTO `messages` VALUES ('m_9263b6ab', '预约申请已提交', '你已提交《12》的预约申请。', '2026-03-26 13:18', 0, 1, '2026-03-26 05:18:24');
INSERT INTO `messages` VALUES ('m_93aac8cd', '预约申请已提交', '你已提交《课堂管理与教学设计》的预约申请。', '2026-03-25 23:48', 0, 1, '2026-03-25 15:48:01');
INSERT INTO `messages` VALUES ('m_97de6e8b', '预约申请已提交', '你已提交《课程名称教研室11》的预约申请。', '2026-04-21 09:12', 0, 3, '2026-04-21 01:12:41');
INSERT INTO `messages` VALUES ('m_a2d127de', '已取消预约', '你已取消《12》的预约。', '2026-03-26 20:43', 0, 1, '2026-03-26 12:43:11');
INSERT INTO `messages` VALUES ('m_a743535e', '预约申请已提交', '你已提交《课程名称1》的预约申请。', '2026-03-26 22:25', 0, 2, '2026-03-26 14:25:43');
INSERT INTO `messages` VALUES ('m_ac2dd625', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 16:07', 0, 2, '2026-04-17 08:07:28');
INSERT INTO `messages` VALUES ('m_afc2abf7', '已取消预约', '你已取消《课程名称1》的预约。', '2026-03-27 09:27', 0, 1, '2026-03-27 01:27:45');
INSERT INTO `messages` VALUES ('m_c0078caa', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 15:51', 0, 2, '2026-04-17 07:51:33');
INSERT INTO `messages` VALUES ('m_c5c0d5c3', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 15:36', 0, 2, '2026-04-17 07:36:34');
INSERT INTO `messages` VALUES ('m_c9716013', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 16:00', 0, 2, '2026-04-17 08:00:37');
INSERT INTO `messages` VALUES ('m_d47d7a6d', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 15:50', 0, 2, '2026-04-17 07:50:58');
INSERT INTO `messages` VALUES ('m_e3532033', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 15:45', 0, 2, '2026-04-17 07:45:04');
INSERT INTO `messages` VALUES ('m_ea2e46cc', '已取消预约', '你已取消《课程名称呃呃呃呃》的预约。', '2026-04-17 15:45', 0, 2, '2026-04-17 07:45:57');
INSERT INTO `messages` VALUES ('m_ed028db5', '已取消预约', '你已取消《出现在曹张新村》的预约。', '2026-04-17 15:50', 0, 2, '2026-04-17 07:50:43');
INSERT INTO `messages` VALUES ('m_fc96c91c', '预约申请已提交', '你已提交《12》的预约申请。', '2026-03-26 20:43', 0, 1, '2026-03-26 12:43:08');
INSERT INTO `messages` VALUES ('m_feaa981f', '预约申请已提交', '你已提交《出现在曹张新村》的预约申请。', '2026-04-17 16:19', 0, 2, '2026-04-17 08:19:04');

-- ----------------------------
-- Table structure for open_courses
-- ----------------------------
DROP TABLE IF EXISTS `open_courses`;
CREATE TABLE `open_courses`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `academic_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `semester` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `week_section` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `capacity` int NOT NULL,
  `enrolled` int NOT NULL,
  `signed_in` int NOT NULL,
  `reviewed` int NOT NULL,
  `applicant` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_open_courses_id`(`id` ASC) USING BTREE,
  INDEX `ix_open_courses_course_name`(`course_name` ASC) USING BTREE,
  INDEX `ix_open_courses_week_section`(`week_section` ASC) USING BTREE,
  INDEX `ix_open_courses_academic_year`(`academic_year` ASC) USING BTREE,
  INDEX `ix_open_courses_semester`(`semester` ASC) USING BTREE,
  INDEX `ix_open_courses_applicant`(`applicant` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of open_courses
-- ----------------------------

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `product_id` int NOT NULL,
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `product_thumbnail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `quantity` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKbioxgbv59vetrxe0ejfubep1w`(`order_id` ASC) USING BTREE,
  INDEX `FKocimc7dtr037rh4ls4l95nlfi`(`product_id` ASC) USING BTREE,
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_items
-- ----------------------------
INSERT INTO `order_items` VALUES (1, 1, 187.00, 3, '我的第一个产品', '/uploads/30a19129-c002-40c0-b520-50b25cb070e9.jpg', 1);
INSERT INTO `order_items` VALUES (2, 2, 187.00, 3, '我的第一个产品', '/uploads/324b3f49-e267-44a1-bbaf-fa767bc8f614.jpg', 1);

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `receiver_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `receiver_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `total_amount` decimal(10, 2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKg8pohnngqi5x1nask7nff2u7w`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, '2026-04-16 09:23:37.211489', 'ORD20260416172337995E8B84', '吴江', 'admin', '13294140902', '擦速度擦拭调查', 'pending', 187.00, '2026-04-16 09:23:37.402981', 1);
INSERT INTO `orders` VALUES (2, '2026-04-17 02:22:44.101560', 'ORD20260417102244D058228A', '上海', 'user', '13294140902', '', 'pending', 187.00, '2026-04-17 02:22:44.259866', 2);

-- ----------------------------
-- Table structure for product_categories
-- ----------------------------
DROP TABLE IF EXISTS `product_categories`;
CREATE TABLE `product_categories`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK8vhlmpa70m0bbxsi64k14389b`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_categories
-- ----------------------------
INSERT INTO `product_categories` VALUES (1, 'clothes', '2026-04-16 03:16:06.907498', '衣服');
INSERT INTO `product_categories` VALUES (2, 'shoe', '2026-04-16 03:16:37.455059', '鞋子');

-- ----------------------------
-- Table structure for products
-- ----------------------------
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `actual_price` decimal(10, 2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `discount` decimal(10, 2) NOT NULL,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `official_price` decimal(10, 2) NOT NULL,
  `poster` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort` int NOT NULL,
  `status` bit(1) NOT NULL,
  `thumbnail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `category_id` int NULL DEFAULT NULL,
  `detail_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '详情图，多张用逗号分隔',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK6t5dtw6tyo83ywljwohuc6g7k`(`category_id` ASC) USING BTREE,
  CONSTRAINT `FK6t5dtw6tyo83ywljwohuc6g7k` FOREIGN KEY (`category_id`) REFERENCES `product_categories` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of products
-- ----------------------------
INSERT INTO `products` VALUES (2, 110.00, '2026-04-16 03:17:28.024235', '111111111111', 1.00, 'shoe', 111.00, '/uploads/2323ddfe-5f17-4fe5-861b-df998a9f6ab4.jpg', 0, b'1', '/uploads/62c47274-5161-4ade-bb1b-9f5cdc2af241.jpg', 1, '/uploads/12df4481-535b-4bb7-9f34-d20ac0e84a27.jpg,/uploads/b40df46b-6faa-4eed-a520-09d7f5b11ed7.jpg');
INSERT INTO `products` VALUES (3, 187.00, '2026-04-16 05:13:27.325239', '21', 1.00, '我的第一个产品', 188.00, '/uploads/86ace731-49a1-4ca3-b249-36cdc8974957.jpg', 2, b'1', '/uploads/61d908e5-710b-42c5-9567-5e7c7dbfb690.jpg', 1, '/uploads/7e3ca660-1294-45ba-a785-ca56a5cd44b9.jpg,/uploads/2a4ca364-94d1-4ee5-b82e-08a9e373b3d7.jpg');
INSERT INTO `products` VALUES (4, 100.00, '2026-04-16 08:54:20.095472', 'casdcasdccsc', 11.00, 'asdcasdc', 111.00, '/uploads/a6b3f7e0-9cd1-41ed-b8cb-4f7caaad80f7.jpg', 1, b'1', '/uploads/802c07d0-4112-4c0f-801e-e33cd8977dd7.jpg', 2, '/uploads/14542bc8-6560-4adf-98e4-68503bcc212a.jpg,/uploads/864d9ccf-7898-4100-b38e-36910f222efd.jpg,/uploads/9b5ae718-78ee-41bb-9b8e-978f07d679df.jpg');

-- ----------------------------
-- Table structure for public_course_applications
-- ----------------------------
DROP TABLE IF EXISTS `public_course_applications`;
CREATE TABLE `public_course_applications`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `academic_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `apply_reason` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `audit_opinion` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `audited_at_str` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `course_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_at_str` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `office` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `section` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `state` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `term` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `topic` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` int NOT NULL,
  `week_no` int NOT NULL,
  `weekday` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of public_course_applications
-- ----------------------------
INSERT INTO `public_course_applications` VALUES ('pca_4f4a3f9dde', '2025-2026', '课题课题课题课题课题课题课题课题课题', 'asascascascasc', '2026-04-17 14:49', '教学观摩', '2024 级 教育学 1 班', '课程名称1', '2026-03-26 13:08:36.577091', '2026-03-26 21:08', '教育学院', '12', '11', '第一节', '2026-03-31', '已发布', '第一学期', '课题', 1, 30, '星期二');
INSERT INTO `public_course_applications` VALUES ('pca_50e58e5fdd', '2025-2026', '课题-称教研室11课题-称教研室11课题-称教研室11', '创新中心中心这些 自行车自行车自行车自行车自行车自行车自行车自行车自行车', '2026-04-09 10:56', '示范课', '2024 级 教育学 2 班', '课程名称教研室11', '2026-03-26 14:18:42.541393', '2026-03-26 22:18', '教育学院', '教研室', '教研室', '第一节', '2026-04-30', '已发布', '第一学期', '课题-称教研室11', 2, 28, '星期四');
INSERT INTO `public_course_applications` VALUES ('pca_6a10cc7f50', '2025-2026', '课题课题课题课题课题课题课题课题课题课题课题', 'vavsdvsdvsdvsdv', '2026-04-09 11:01', '示范课', '2023 级 计算机 1 班', '课程名称呃呃呃呃', '2026-04-09 03:01:38.485711', '2026-04-09 11:01', '信息工程学院', '开课地点', '教研室', '第六节', '2026-04-30', '已发布', '第一学期', '课题1', 2, 2, '星期四');
INSERT INTO `public_course_applications` VALUES ('pca_cadd48291b', '2025-2026', '195测试课题195测试课题195测试课题195测试课题', '', '', '示范课', '2023 级 计算机 1 班', '195测试课题', '2026-04-21 01:24:40.821522', '2026-04-21 09:24', '信息工程学院', 'xxxxx', 'xxxx', '第二节', '2026-04-30', '待审批', '第一学期', '195测试课题', 3, 2, '星期四');
INSERT INTO `public_course_applications` VALUES ('pca_dfb1d3fe85', '2025-2026', '测试v大苏打VS的VS的v', '从撒旦擦拭擦速度擦拭调查是', '2026-03-27 09:18', '教学观摩', '2024 级 软件工程 1 班', '伟大我去', '2026-03-27 01:16:19.623156', '2026-03-27 09:16', '信息工程学院', '伟大我去', '伟大我去', '第二节', '2026-04-02', '已发布', '第一学期', '伟大我去', 2, 7, '星期四');
INSERT INTO `public_course_applications` VALUES ('pca_e74a9fbb79', '2025-2026', '出现在曹张新村', 'vhvhvhhhvvhhv', '2026-04-17 14:48', '教学观摩', '2024 级 英语 1 班', '出现在曹张新村', '2026-04-09 06:48:34.223978', '2026-04-09 14:48', '外国语学院', '出现在曹张新村', '出现在曹张新村', '第三节', '2026-04-30', '已发布', '第一学期', '出现在曹张新村', 2, 1, '星期四');
INSERT INTO `public_course_applications` VALUES ('pca_fa92928f58', '2025-2026', 'ddqwdqwd课题', 'svsdvsd', '2026-03-26 22:22', '研讨课', '2024 级 英语 1 班', '课程名称-0教研室12212', '2026-03-26 14:21:30.989658', '2026-03-26 22:21', '外国语学院', '开课地点-0教研室12212', '教研室12', '第一节', '2026-03-31', '已发布', '第一学期', '课题-0教研室12212', 1, 28, '星期二');

-- ----------------------------
-- Table structure for public_course_reviews
-- ----------------------------
DROP TABLE IF EXISTS `public_course_reviews`;
CREATE TABLE `public_course_reviews`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `course_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of public_course_reviews
-- ----------------------------
INSERT INTO `public_course_reviews` VALUES (2, 'scascascascasc', 'pca_4f4a3f9dde', '2026-03-26 14:35:15.575983', 2);

-- ----------------------------
-- Table structure for public_courses
-- ----------------------------
DROP TABLE IF EXISTS `public_courses`;
CREATE TABLE `public_courses`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tab` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `time` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `speaker` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_public_courses_title`(`title` ASC) USING BTREE,
  INDEX `ix_public_courses_tab`(`tab` ASC) USING BTREE,
  INDEX `ix_public_courses_status`(`status` ASC) USING BTREE,
  INDEX `ix_public_courses_time`(`time` ASC) USING BTREE,
  INDEX `ix_public_courses_id`(`id` ASC) USING BTREE,
  INDEX `ix_public_courses_speaker`(`speaker` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of public_courses
-- ----------------------------
INSERT INTO `public_courses` VALUES ('p_l_1', 'learning', '我听课：信息化课堂（示例）', '2026-03-21 09:30', '赵老师', '已结束', '2026-03-26 13:14:24');
INSERT INTO `public_courses` VALUES ('p_l_2', 'learning', '我听课：班级管理（示例）', '2026-03-28 13:30', '钱老师', '未开始', '2026-03-26 13:14:24');
INSERT INTO `public_courses` VALUES ('p_r_1', 'reviewing', '我评课：公开课评课（示例）', '2026-03-20 16:00', '孙老师', '待提交', '2026-03-26 13:14:24');
INSERT INTO `public_courses` VALUES ('p_t_1', 'teaching', '我开课：教学观摩课（示例）', '2026-03-25 10:00', '我', '进行中', '2026-03-26 13:14:24');
INSERT INTO `public_courses` VALUES ('p_t_2', 'teaching', '我开课：课堂提问设计（示例）', '2026-03-29 15:00', '我', '未开始', '2026-03-26 13:14:24');

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKofx66keruapi6vyqpv6f2or37`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of roles
-- ----------------------------
INSERT INTO `roles` VALUES ('r_admin', '2026-03-26 04:16:35.244573', '管理员：管理端功能', 'admin');
INSERT INTO `roles` VALUES ('r_user', '2026-03-26 04:16:35.244573', '普通用户：仅可查看和提交申请', 'user');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `hashed_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `birth_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `ix_users_username`(`username` ASC) USING BTREE,
  INDEX `ix_users_id`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '$pbkdf2-sha256$29000$rNX6v/fe29u7t5Zyzvk/Rw$yaKT0vD5i.TNyRf7D8S1j.nzSOVq644raOTQTbvo7q0', 1, '2026-03-23 07:57:01', 'admin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (2, 'user', '$pbkdf2-sha256$29000$lY7mfMfRrSD4j01uPUtnbA$fYerB5nK2yWRLkCPlcw4bqCc7v31ZyhBHuemr01yUEI', 1, '2026-03-23 08:19:38', 'user', '地址地址地址地址', '2026-05-05', '男', '13294140902', NULL, NULL);
INSERT INTO `users` VALUES (3, 'cc', '$pbkdf2-sha256$29000$LmjGZu6ZwvNuCeUVuV..vA$zumB8NGbQ.z71DEmG9EXRBLDuSw57ik9iCr8E4iITTY', 1, '2026-03-23 09:07:48', 'user', '地址', '2026-05-25', '男', '19532152272', NULL, NULL);
INSERT INTO `users` VALUES (4, 'cc1', '$pbkdf2-sha256$29000$A18K/W/HiOh1o10xo2PHDQ$e8x4mI/n.188DVrhCoxBveXLvjrg1GE/DzvS53anbi4', 0, '2026-03-25 15:26:06', 'user', NULL, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
