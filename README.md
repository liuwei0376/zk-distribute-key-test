# zk-distribute-key-test

### 一、分布式锁实现思路

线程安全的一般控制方法：
> 对公共资源进行控制：
> - 同步
> - 加锁等

随着程序逐渐分布式化，由以前单进程多线程的，演变为现在的多进程多线程，故使用原有同步、加锁机制不能很好的支撑。
使用分布式锁解决这类问题。


### 二、场景：电商下单
在电商下单业务场景中，一般会有如下流程：
- ①、根据用户下单商品，查该商品的库存。如果库存不足，提示库存不足；如果库存充足，则进入下个流程；
- ②、订单表中添加一条记录（订单表与子订单表1：n的关系）；
- ③、去商品表中进行库存扣除。

如果网站用户量不大，并发不高的情况下，可能系统状态良好。

但是用户一旦上量，加之原先系统并未考虑线程同步、加锁等机制，则很有可能会发生各种线程安全性问题，典型的有：
- ①、多用户并发下单，导致系统库存混乱。
- ②、前端提示下单成功，库存却无货，无法发货。



### 三、数据准备

#### 3.1 构建数据库：

> 说明：
>>   本项目采用Springboot + SpringDataJPA方式构建，建表语句无需手工在dbms（这里为MySQL）中构建，
>>这里仅仅展示表结构。

3.1.1 建表scripts
```
-- ----------------------------
-- Table structure for items
-- ----------------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `id` varchar(255) COLLATE utf8_bin NOT NULL,
  `counts` int(11) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Records of items
-- ----------------------------
INSERT INTO `items` VALUES ('1', '10', 'Spark');
INSERT INTO `items` VALUES ('2', '6', 'Hadoop');
INSERT INTO `items` VALUES ('3', '2', 'Flink');
```


```
-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` varchar(255) COLLATE utf8_bin NOT NULL,
  `item_id` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES ('0f36a090-9ad0-4060-9e4d-347ded7a6ebd', '1');
```



3.1.2 表数据样例

订单表：
```
orders表：
----------------
    id  itemid
    1   1 
```
 
商品条目表：
```
items表：
----------------
    id  name    counts
    1   Spark   10
    2   hdaoop  6
    3   Flink   3
```


### 四、 软件环境
> 
- SpringBoot2.x
- spring-boot-starter-data-jpa
- MySQL（示例为本地单机）
- Zookeeper（示例为本地单机）


### 五、 测试方法

浏览器打开两个个tab页，分别同时并发进行请求，模拟高并发场景下，使用zk分布式锁解决线程的并发问题：

> http://localhost:8081/pay/buy1?itemId=1
http://localhost:8081/pay/buy2?itemId=1