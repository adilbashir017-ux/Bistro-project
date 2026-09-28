CREATE DATABASE  IF NOT EXISTS `db_bistro` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `db_bistro`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: db_bistro
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `openhours`
--

DROP TABLE IF EXISTS `openhours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `openhours` (
  `day` varchar(45) NOT NULL,
  `open` time NOT NULL,
  `close` time NOT NULL,
  PRIMARY KEY (`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `openhours`
--

LOCK TABLES `openhours` WRITE;
/*!40000 ALTER TABLE `openhours` DISABLE KEYS */;
INSERT INTO `openhours` VALUES ('1','00:00:01','23:59:59'),('2','15:00:00','21:00:00'),('3','15:00:00','21:00:00'),('4','15:00:00','21:00:00'),('5','15:00:00','21:00:00'),('6','15:00:00','22:00:00'),('7','00:00:01','23:59:59');
/*!40000 ALTER TABLE `openhours` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `order_number` int NOT NULL AUTO_INCREMENT,
  `order_date` date NOT NULL,
  `order_time` time NOT NULL,
  `arrival_datetime` datetime DEFAULT NULL,
  `number_of_guests` int NOT NULL,
  `confirmation_code` int NOT NULL,
  `subscriber_id` int DEFAULT NULL,
  `customer_name` varchar(100) NOT NULL,
  `customer_email` varchar(255) NOT NULL,
  `customer_phone` varchar(20) NOT NULL,
  `date_of_placing_order` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `table_num` int DEFAULT NULL,
  `order_status` enum('BOOKED','SEATED','BILL_SENT','WAITING','WAITING_CALLED','WAITING_SEATED','COMPLETED','PAID','CANCELLED_BY_USER','CANCELLED_NO_SHOW','CANCELLED_BY_STAFF') NOT NULL,
  `status_datetime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reminder_sent` tinyint DEFAULT '0',
  PRIMARY KEY (`order_number`),
  UNIQUE KEY `confirmation_code` (`confirmation_code`),
  KEY `fk_order_subscriber` (`subscriber_id`),
  KEY `fk_order_table` (`table_num`),
  CONSTRAINT `fk_order_subscriber` FOREIGN KEY (`subscriber_id`) REFERENCES `subscriber` (`subscriber_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_order_table` FOREIGN KEY (`table_num`) REFERENCES `tables` (`table_num`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_guests_positive` CHECK ((`number_of_guests` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order`
--

LOCK TABLES `order` WRITE;
/*!40000 ALTER TABLE `order` DISABLE KEYS */;
INSERT INTO `order` VALUES (20,'2026-01-16','15:40:00',NULL,2,788036,18,'DemoCustomerB','demo8@example.com','0500000008','2026-01-16 15:32:09',1,'BILL_SENT','2026-01-16 15:34:43',0),(21,'2026-01-22','17:30:00',NULL,2,415150,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-16 15:46:49',NULL,'BOOKED','2026-01-16 15:46:49',0),(22,'2026-01-30','16:30:00',NULL,2,754247,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-16 15:46:55',NULL,'BOOKED','2026-01-16 15:46:55',0),(23,'2026-01-28','18:00:00',NULL,2,796119,18,'DemoCustomerB','demo8@example.com','0500000008','2026-01-16 21:10:02',NULL,'BOOKED','2026-01-16 21:10:02',0),(24,'2026-01-22','15:00:00',NULL,2,340730,18,'DemoCustomerB','demo8@example.com','0500000008','2026-01-16 21:23:21',NULL,'BOOKED','2026-01-16 21:23:21',0),(25,'2026-01-28','15:00:00',NULL,2,413905,18,'DemoCustomerB','demo8@example.com','0500000008','2026-01-16 21:47:09',NULL,'BOOKED','2026-01-16 21:47:09',0),(26,'2026-01-30','15:00:00',NULL,2,221460,18,'DemoCustomerB','demo8@example.com','0500000008','2026-01-16 22:47:13',NULL,'BOOKED','2026-01-16 22:47:13',0),(27,'2026-01-17','20:00:00',NULL,5,178903,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-17 19:50:49',7,'BILL_SENT','2026-01-17 19:51:22',1),(28,'2026-01-17','20:38:16',NULL,2,770747,NULL,'akiuy','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 20:38:16',0),(29,'2026-01-17','20:38:20',NULL,2,534460,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 20:38:20',0),(30,'2026-01-17','20:38:21',NULL,2,566553,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 20:38:22',0),(31,'2026-01-17','20:38:25',NULL,3,738906,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 20:38:25',0),(32,'2026-01-17','20:38:36',NULL,2,728392,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',4,'BILL_SENT','2026-01-17 20:38:36',0),(33,'2026-01-17','20:38:38',NULL,2,863963,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',1,'BILL_SENT','2026-01-17 20:45:09',0),(34,'2026-01-17','20:38:44',NULL,2,120841,NULL,'akiuy6','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'BILL_SENT','2026-01-17 20:50:11',0),(35,'2026-01-17','20:41:16',NULL,2,192062,NULL,'akiuy654','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'CANCELLED_BY_USER','2026-01-17 20:47:44',0),(36,'2026-01-17','20:41:21',NULL,2,284976,NULL,'akiuy6546','demo3@example.com','0500000002','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 20:56:06',0),(37,'2026-01-19','16:00:00',NULL,3,350520,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-17 22:30:21',NULL,'BOOKED','2026-01-17 22:30:20',0),(38,'2026-01-17','22:38:00','2026-01-17 22:37:59',2,291888,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-17 22:35:55',NULL,'BILL_SENT','2026-01-17 22:37:59',1),(39,'2026-01-29','15:00:00',NULL,4,410721,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-17 23:07:20',NULL,'BOOKED','2026-01-17 23:07:19',0),(40,'2026-01-17','23:16:00','2026-01-17 23:11:11',2,111111,19,'DemoCustomerA','demo1@example.com','0500000007','2026-01-16 15:46:55',NULL,'PAID','2026-01-17 23:13:27',1),(41,'2026-01-17','01:00:00',NULL,2,260543,NULL,'ffads','demo7@example.com','0500000004','2026-01-17 00:00:00',NULL,'PAID','2026-01-17 23:53:07',0),(42,'2026-01-17','01:00:00',NULL,2,212565,NULL,'ffads','demo7@example.com','0500000004','2026-01-17 00:00:00',1,'BILL_SENT','2026-01-17 23:52:10',0),(43,'2026-01-17','01:00:00',NULL,2,612326,NULL,'ffads','demo7@example.com','0500000004','2026-01-17 00:00:00',1,'BILL_SENT','2026-01-17 23:52:11',0),(47,'2026-01-17','01:00:00',NULL,2,444834,NULL,'ffads','demo7@example.com','0500000004','2026-01-17 00:00:00',1,'BILL_SENT','2026-01-17 23:52:14',0),(50,'2026-01-17','01:00:00','2026-01-17 23:53:25',2,232902,NULL,'ffads21','demo7@example.com','0500000004','2026-01-17 00:00:00',NULL,'BILL_SENT','2026-01-17 23:53:25',0),(51,'2026-01-18','00:30:00',NULL,2,261447,NULL,'afsd','demo6@example.com','0500000005','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:17:38',0),(52,'2026-01-18','00:30:00',NULL,2,354391,NULL,'afsd3','demo6@example.com','0500000005','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:20:27',0),(53,'2026-01-18','00:30:00','2026-01-18 00:19:37',2,707531,NULL,'afsd32','demo6@example.com','0500000005','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:25:03',0),(54,'2026-01-18','00:30:00',NULL,2,596189,NULL,'afsd322','demo6@example.com','0500000005','2026-01-18 00:00:00',NULL,'CANCELLED_BY_USER','2026-01-18 00:22:05',0),(55,'2026-01-18','00:30:00','2026-01-18 00:25:52',2,582658,NULL,'afsd3224','demo6@example.com','0500000005','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:26:19',0),(56,'2026-01-18','00:00:00',NULL,2,960045,NULL,'sad','demo2@example.com','0500000006','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:36:41',0),(57,'2026-01-18','00:00:00',NULL,2,295875,NULL,'sad2','demo2@example.com','0500000006','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:41:51',0),(58,'2026-01-18','00:00:00','2026-01-18 00:42:18',2,846515,NULL,'sad21','demo2@example.com','0500000006','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:44:06',0),(59,'2026-01-18','00:00:00','2026-01-18 00:45:33',2,608177,NULL,'sad212','demo2@example.com','0500000006','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:46:04',0),(60,'2026-01-18','00:30:00','2026-01-18 00:45:46',2,143403,NULL,'fdsfsd','demo4@example.com','0500000003','2026-01-18 00:00:00',NULL,'PAID','2026-01-18 00:48:16',0);
/*!40000 ALTER TABLE `order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `payment_id` int NOT NULL AUTO_INCREMENT,
  `order_number` int NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `order_number_UNIQUE` (`order_number`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_number`) REFERENCES `order` (`order_number`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
INSERT INTO `payment` VALUES (7,28,100.00),(8,29,100.00),(9,30,100.00),(10,31,150.00),(11,36,100.00),(12,40,100.00),(13,41,100.00),(14,51,100.00),(15,52,100.00),(16,53,100.00),(17,55,100.00),(18,56,100.00),(19,57,100.00),(20,58,100.00),(21,59,100.00),(22,60,100.00);
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `specialdays`
--

DROP TABLE IF EXISTS `specialdays`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `specialdays` (
  `special_id` int NOT NULL AUTO_INCREMENT,
  `special_date` date NOT NULL,
  `is_all_day_close` tinyint(1) NOT NULL DEFAULT '0',
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`special_id`),
  KEY `idx_special_date` (`special_date`),
  CONSTRAINT `specialdays_chk_1` CHECK ((((`is_all_day_close` = 1) and (`start_time` is null) and (`end_time` is null)) or ((`is_all_day_close` = 0) and (`start_time` is not null) and (`end_time` is not null) and (`start_time` < `end_time`))))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `specialdays`
--

LOCK TABLES `specialdays` WRITE;
/*!40000 ALTER TABLE `specialdays` DISABLE KEYS */;
INSERT INTO `specialdays` VALUES (1,'2026-01-30',1,NULL,NULL,'Holiday – restaurant closed all day'),(2,'2026-02-14',0,'18:00:00','21:00:00','Valentine Special Hours'),(3,'2026-12-25',1,NULL,NULL,'Christmas – closed'),(4,'2026-07-04',0,'17:00:00','22:00:00','Independence Day special hours');
/*!40000 ALTER TABLE `specialdays` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff`
--

DROP TABLE IF EXISTS `staff`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff` (
  `staff_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `password` varchar(45) NOT NULL,
  `role` enum('MANAGER','AGENT','REPRESENTATIVE') NOT NULL,
  PRIMARY KEY (`staff_id`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff`
--

LOCK TABLES `staff` WRITE;
/*!40000 ALTER TABLE `staff` DISABLE KEYS */;
INSERT INTO `staff` VALUES (1,'manager_demo','demo123','MANAGER'),(2,'agent_demo','demo123','AGENT');
/*!40000 ALTER TABLE `staff` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subscriber`
--

DROP TABLE IF EXISTS `subscriber`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscriber` (
  `subscriber_id` int NOT NULL AUTO_INCREMENT,
  `subscriber_name` varchar(45) NOT NULL,
  `subscriber_email` varchar(255) DEFAULT NULL,
  `subscriber_phone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`subscriber_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subscriber`
--

LOCK TABLES `subscriber` WRITE;
/*!40000 ALTER TABLE `subscriber` DISABLE KEYS */;
INSERT INTO `subscriber` VALUES (1,'DemoSubscriberA','demo5@example.com','0500000001'),(18,'DemoCustomerB','demo8@example.com','0500000008'),(19,'DemoCustomerA','demo1@example.com','0500000007');
/*!40000 ALTER TABLE `subscriber` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tables`
--

DROP TABLE IF EXISTS `tables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tables` (
  `table_num` int NOT NULL,
  `places` int NOT NULL,
  PRIMARY KEY (`table_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tables`
--

LOCK TABLES `tables` WRITE;
/*!40000 ALTER TABLE `tables` DISABLE KEYS */;
INSERT INTO `tables` VALUES (1,2),(4,2),(5,4),(6,4),(7,6),(8,6),(9,6),(10,10);
/*!40000 ALTER TABLE `tables` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'db_bistro'
--

--
-- Dumping routines for database 'db_bistro'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-18  2:49:59
