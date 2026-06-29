# Redis Java 实现 TODO

## 时间评估

> **一个周末不够**，诚实估算如下：

| 阶段 | 预计时间 |
|---|---|
| 数据结构（dict + skiplist + intset） | 12–16 h |
| 对象系统 | 4 h |
| RESP 协议 | 4 h |
| 事件循环 + 网络层 | 8–10 h |
| 命令实现（主要类型） | 16–20 h |
| 数据库层（过期、多 DB） | 4–6 h |
| RDB 持久化 | 8 h |
| AOF 持久化 | 10 h |
| **合计** | **~70 h** |

**一个周末（约 16–20 h）能完成的现实目标：**
- dict + skiplist + intset
- RESP 解析器
- 最小可跑的 server（能用 redis-cli 连上）
- GET / SET / DEL / PING / EXPIRE

把这个当周末目标，剩下的利用后续 3–4 个周末完成。

---

## Phase 1｜数据结构（预计 12–16 h）

### 1.1 字典 dict ✦ 最高优先级
- [ ] `DictEntry<K, V>`：键值对节点，含 next 指针（拉链法）
- [ ] `DictHashTable<K, V>`：哈希表，含 table 数组、size、sizemask、used
- [ ] `Dict<K, V>`：两张哈希表（ht[0] 正常用，ht[1] rehash 用）
- [ ] `put` / `get` / `delete` 基本操作
- [ ] 渐进式 rehash：
    - [ ] 触发条件（负载因子 > 1 时扩容，< 0.1 时缩容）
    - [ ] `rehashStep()`：每次操作时迁移 1 个桶
    - [ ] rehash 期间读写同时查 ht[0] 和 ht[1]
- [ ] 单元测试：put/get/delete、扩容触发、rehash 正确性

### 1.2 跳表 skiplist
- [ ] `SkipListNode<V>`：含 score、value、层数组（每层有 forward 指针和 span）
- [ ] `SkipList<V>`：header 哨兵节点、tail 指针、length、level
- [ ] `insert(score, value)`：随机层数（p=0.25）
- [ ] `delete(score, value)`
- [ ] `rangeByScore(min, max)`：按分数范围查找
- [ ] `rangeByRank(start, end)`：按排名查找（利用 span）
- [ ] 单元测试：插入排序正确性、范围查询、rank 计算

### 1.3 整数集合 intset
- [ ] 底层用 `long[]` 存储（统一用最大精度，简化升级逻辑）
- [ ] 保持有序，二分查找
- [ ] `add(value)`：查重 + 有序插入
- [ ] `remove(value)`
- [ ] `contains(value)`
- [ ] 单元测试：有序性、查重、边界值

---

## Phase 2｜对象系统（预计 4 h）

- [ ] `RedisType` 枚举：STRING / LIST / HASH / SET / ZSET
- [ ] `RedisEncoding` 枚举：INT / EMBSTR / RAW / ZIPLIST / LISTPACK / HT / LINKEDLIST / SKIPLIST / INTSET
- [ ] `RedisObject`：type + encoding + value（Object）
- [ ] 编码选择策略（小数据用紧凑编码，超阈值升级）：
    - [ ] List：元素数 ≤ 128 且单元素 ≤ 64 字节 → ziplist，否则 → linkedlist
    - [ ] Hash：同上 → ziplist，否则 → hashtable
    - [ ] Set：全整数且数量 ≤ 512 → intset，否则 → hashtable
    - [ ] ZSet：数量 ≤ 128 且单元素 ≤ 64 字节 → ziplist，否则 → skiplist+hashtable
- [ ] 单元测试：编码升级触发条件

---

## Phase 3｜RESP 协议（预计 4 h）

协议格式参考：https://redis.io/docs/reference/protocol-spec/

- [ ] `RespParser`：从 ByteBuffer 解析 RESP 数据
    - [ ] Simple String：`+OK\r\n`
    - [ ] Error：`-ERR message\r\n`
    - [ ] Integer：`:1000\r\n`
    - [ ] Bulk String：`$6\r\nfoobar\r\n`，`$-1\r\n`（null）
    - [ ] Array：`*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n`
    - [ ] 处理半包（数据未读完时挂起，等下次读事件）
- [ ] `RespWriter`：将 Java 对象序列化为 RESP 格式
- [ ] 单元测试：各类型解析、半包重组、null bulk string

---

## Phase 4｜网络层 + 事件循环（预计 8–10 h）

**核心：用 Java NIO 实现 Reactor 单线程模型，对应 Redis 的 ae 事件库**

- [ ] `EventLoop`：
    - [ ] `Selector` 管理所有 Channel
    - [ ] `run()` 主循环：`select()` → 处理 I/O 事件 → 执行定时任务
    - [ ] 注册/注销读写事件
    - [ ] 定时任务队列（用于 activeExpire、AOF flush）
- [ ] `RedisServer`：
    - [ ] 启动 `ServerSocketChannel`，绑定端口（默认 6379）
    - [ ] accept 新连接，注册到 EventLoop
- [ ] `RedisClient`：
    - [ ] 每个连接对应一个 Client 对象
    - [ ] 读缓冲区（接收数据）+ 写缓冲区（待发送数据）
    - [ ] 当前选中的数据库 id（默认 0）
    - [ ] 事务状态（MULTI/EXEC，后期实现）
- [ ] 读事件处理：读数据 → RespParser 解析 → 得到完整命令 → 执行 → 写响应到写缓冲区
- [ ] 写事件处理：将写缓冲区数据 flush 到 channel

---

## Phase 5｜命令实现（预计 16–20 h）

- [ ] `CommandTable`：命令名 → CommandHandler 的映射
- [ ] `CommandHandler` 接口：`execute(RedisClient client, String[] args)`

### 5.1 通用命令
- [ ] `PING`
- [ ] `SELECT db`
- [ ] `DEL key [key ...]`
- [ ] `EXISTS key`
- [ ] `TYPE key`
- [ ] `EXPIRE key seconds` / `EXPIREAT key timestamp`
- [ ] `TTL key` / `PTTL key`
- [ ] `PERSIST key`
- [ ] `RENAME key newkey`
- [ ] `KEYS pattern`（简单通配符匹配）
- [ ] `DBSIZE`
- [ ] `FLUSHDB` / `FLUSHALL`

### 5.2 String 命令
- [ ] `SET key value [EX seconds] [NX|XX]`
- [ ] `GET key`
- [ ] `MSET` / `MGET`
- [ ] `INCR` / `INCRBY` / `DECR` / `DECRBY`
- [ ] `APPEND key value`
- [ ] `STRLEN key`
- [ ] `SETNX` / `SETEX` / `GETSET`

### 5.3 List 命令
- [ ] `LPUSH` / `RPUSH` / `LPUSHX` / `RPUSHX`
- [ ] `LPOP` / `RPOP`
- [ ] `LRANGE key start stop`
- [ ] `LLEN key`
- [ ] `LINDEX key index`
- [ ] `LSET key index value`
- [ ] `LINSERT key BEFORE|AFTER pivot value`
- [ ] `LREM key count value`

### 5.4 Hash 命令
- [ ] `HSET key field value`
- [ ] `HGET key field`
- [ ] `HMSET` / `HMGET`
- [ ] `HDEL key field`
- [ ] `HEXISTS key field`
- [ ] `HGETALL key`
- [ ] `HKEYS` / `HVALS`
- [ ] `HLEN key`
- [ ] `HINCRBY key field increment`

### 5.5 Set 命令
- [ ] `SADD key member [member ...]`
- [ ] `SREM key member`
- [ ] `SMEMBERS key`
- [ ] `SISMEMBER key member`
- [ ] `SCARD key`
- [ ] `SINTER` / `SUNION` / `SDIFF`
- [ ] `SRANDMEMBER key [count]`

### 5.6 ZSet 命令
- [ ] `ZADD key score member`
- [ ] `ZREM key member`
- [ ] `ZSCORE key member`
- [ ] `ZRANK key member` / `ZREVRANK`
- [ ] `ZRANGE key start stop [WITHSCORES]`
- [ ] `ZREVRANGE key start stop`
- [ ] `ZRANGEBYSCORE key min max`
- [ ] `ZCARD key`
- [ ] `ZINCRBY key increment member`

---

## Phase 6｜数据库层（预计 4–6 h）

- [ ] `RedisDb`：
    - [ ] `dict`：主键空间（`Dict<String, RedisObject>`）
    - [ ] `expires`：过期时间表（`Dict<String, Long>`，存毫秒时间戳）
    - [ ] 16 个数据库（`RedisDb[]`，默认 16 个）
- [ ] 惰性过期（lazy expire）：访问 key 时检查是否过期，过期则删除后返回 null
- [ ] 主动过期（active expire）：EventLoop 定时任务，每 100ms 随机抽样检查过期 key
- [ ] `OBJECT ENCODING key`：查看实际编码（调试用）

---

## Phase 7｜RDB 持久化（预计 8 h）

- [ ] **文件格式**（自定义二进制，无需完全兼容 Redis）：
  ```
  [magic: "REDIS"] [version: 4 bytes]
  [select db: 1 byte] [db index: varint]
  [key-value pairs...]
  [EOF marker]
  [CRC64 checksum: 8 bytes]
  ```
- [ ] `RdbWriter`：将 RedisDb 序列化到临时文件
    - [ ] String：整数直接存，其他 UTF-8 bytes 前缀长度
    - [ ] List / Set / Hash / ZSet：先写元素数量，再逐个序列化
    - [ ] 写完后 `Files.move(tmp, target, ATOMIC_MOVE)`
- [ ] `RdbReader`：从文件反序列化，重建数据库状态
- [ ] `SAVE`：同步保存（阻塞主线程，简单）
- [ ] `BGSAVE`：后台线程保存
    - [ ] 在事件循环安全点深拷贝键空间
    - [ ] 提交后台线程序列化
    - [ ] `LASTSAVE` 命令返回最后保存时间
- [ ] 启动时自动加载 `dump.rdb`
- [ ] 配置：`save 900 1`（900 秒内有 1 次写则自动保存）

---

## Phase 8｜AOF 持久化（预计 10 h）

- [ ] `AofManager`：
    - [ ] `writeBuffer`：主线程追加命令（`ByteArrayOutputStream`）
    - [ ] `FileChannel`：AOF 文件句柄
    - [ ] fsync 策略枚举：ALWAYS / EVERYSEC / NO
- [ ] 命令执行后追加 RESP 格式到 writeBuffer
- [ ] `everysec` 后台定时任务：drain buffer → `channel.write()` → `channel.force(true)`
- [ ] `always`：每条命令后同步 `force(true)`
- [ ] 启动时 AOF 加载：逐条解析 RESP 命令并执行重放
- [ ] AOF 重写（`BGREWRITEAOF`）：
    - [ ] 设置 `rewriting = true`，开启重写缓冲区
    - [ ] 后台线程遍历 db，为每个 key 生成最小化恢复命令
    - [ ] 重写完成：追加重写缓冲区 → `force(true)` → `ATOMIC_MOVE`
    - [ ] 设置 `rewriting = false`
- [ ] 配置：`appendonly yes` / `appendfsync everysec`

---

## 后续可选扩展

- [ ] `MULTI` / `EXEC` / `DISCARD`（事务）
- [ ] `SUBSCRIBE` / `PUBLISH`（发布订阅）
- [ ] `OBJECT ENCODING` / `DEBUG OBJECT`（调试命令）
- [ ] RDB + AOF 混合持久化（Redis 4.0 特性）
- [ ] `INFO` 命令（服务器状态信息）
- [ ] `CONFIG GET` / `CONFIG SET`（运行时配置）

---

## 周末可达目标（16–20 h）

```
Day 1（上午）  dict 实现 + 测试          4 h
Day 1（下午）  skiplist + intset          4 h
Day 1（晚上）  RESP 协议解析器            3 h
Day 2（上午）  EventLoop + RedisServer    4 h
Day 2（下午）  GET / SET / DEL / PING     3 h
Day 2（晚上）  EXPIRE + 惰性过期 + 联调   2 h
```

完成后可以用 `redis-cli -p 6379` 连上自己的服务，跑基本命令。