# Redis-practice  
  
## Redis数据结构、原理分析、应用实战  
  
- [什么是Redis](#什么是redis)  
- [Redis的作用](#redis的作用)  
- [Redis的存储结构](#redis的存储结构)  
- [Redis的安装](#redis的安装)  
- [Redis的数据类型](#redis的数据类型)  
  - [字符串类型](#字符串类型)  
  - [列表类型](#列表类型)  
  - [hash类型](#hash类型)  
  - [集合类型](#集合类型)  
  - [有序集合](#有序集合)  
- [Redis原理分析](#redis原理分析)  
  - [过期时间设置](#过期时间设置)  
  - [过期删除的原理](#过期删除的原理)  
  - [发布订阅](#发布订阅)  
  - [数据持久化](#数据持久化)    
  - [内存回收策略](#内存回收策略)  
  - [单线程高性能原理](#单线程高性能原理)
- [在Redis中使用Lua脚本](#在redis中使用lua脚本)  
  - [原子性问题](#原子性问题)  
  - [效率问题](#效率问题)  
  - [Lua](#lua)  
  - [Redis与Lua](#redis与lua)  
- [Redis集群](#redis集群)  
  - [主从复制](#主从复制)  
  - [主从复制原理](#主从复制原理)  
  - [Redis-Cluster](#rediscluster)  
- [Redis Java API 操作方法及原理分析](#redis-java-api-操作方法及原理分析)  
  - [Jedis-sentinel原理分析](#jedissentinel原理分析)  
  - [Jedis-cluster原理分析](#jediscluster原理分析)  
  - [Redisson的操作方式](#redisson的操作方式)  
- [Redis实战及源码分析](#redis实战及源码分析)  
  - [分布式锁实战](#分布式锁实战)  
  - [管道模式](#管道模式)  
- [Redis的应用架构](#redis的应用架构)  
  - [缓存与数据一致性问题](#缓存与数据一致性问题)  
  - [缓存雪崩与缓存穿透](#缓存雪崩与缓存穿透)  
  
### 什么是Redis  
  
Redis是一个开源的使用ANSI C语言编写、支持网络、可基于内存亦可持久化的日志型、Key-Value数据库，并提供多种语言的API。从2010年3月15日起，Redis的开发工作由VMware主持。从2013年5月开始，Redis的开发由Pivotal赞助。  
  
### Redis的作用  
  
缓存大致可以分为两类，一种是应用内缓存，比如Map(简单的数据结构)，以及EH Cache(Java第三方库)，另一种就是缓存组件，比如Memached；Redis；   
Redis(remote dictionary server)是一个基于KEY-VALUE的高性能的存储系统，通过提供多种键值数据类型来适应不同场景下的缓存与存储需求。Redis默认支持16个“数据库”，即db分为16个部分，类似于命名空间，并不是完全的隔离，我们可以在任意一个命名空间中创建key和value。
  
### Redis的存储结构  
  
大家一定对字典类型的数据结构非常熟悉，比如map ，通过key value的方式存储的结构。redis的全称是remote dictionary server(远程字典服务器)，它以字典结构存储数据，并允许其他应用通过TCP协议读写字典中的内容。数据结构如下  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%AD%98%E5%82%A8%E7%BB%93%E6%9E%84.jpeg)  
  
### Redis的安装  
redis约定次版本号(第一个小数点后的数字)为偶数版本是稳定版，如2.8、3.0，奇数版本为非稳定版，生产环境需要使用稳定版；本文使用3.2版本。  
  
安装配置  
>1.下载redis的安装包  
2.tar -zxvf 解压  
3.cd 到解压后的目录  
4.执行make 完成编译  
5.make test 测试编译状态  
6.make install {PREFIX=/path}  
  
启动停止Redis  
安装完redis后的下一步就是怎么去启动和访问，我们首先先了解一下Redis包含哪些可执行文件  
```
Redis-server        Redis服务器
Redis-cli           Redis命令行客户端
Redis-benchmark     Redis性能测试工具
Redis-check-aof     Aof文件修复工具
Redis-check-dump    Rdb文件检查工具
Redis-sentinel      Sentinel服务器(2.8以后)
 ```
我们常用的命令是redis-server和redis-cli  

1.直接启动  
redis-server ../redis.conf  
服务器启动后默认使用的是6379的端口，通过--port可以自定义端口；  
  
Redis-server --port 6380  
以守护进程的方式启动，需要修改redis.conf配置文件中daemonize yes  

2.停止redis  
redis-cli SHUTDOWN  
考虑到redis有可能正在将内存的数据同步到硬盘中，强行终止redis进程可能会导致数据丢失，正确停止redis的方式应该是向Redis发送SHUTDOW命令。
当redis收到SHUTDOWN命令后，会先断开所有客户端连接，然后根据配置执行持久化，最终完成退出。  
  
### Redis的数据类型  
   
#### 字符串类型 
字符串类型是redis中最基本的数据类型，它能存储任何形式的字符串，包括二进制数据。你可以用它存储用户的邮箱、json化的对象甚至是图片。一个字符类型键允许存储的最大容量是512M。  

数据结构  
在Redis内部，String类型通过 int、SDS(simple dynamic string)作为结构存储，int用来存放整型数据，sds存放字节/字符串和浮点型数据。在C的标准字符串结构下进行了封装，用来提升基本操作的性能，同时也充分利用已有的C的标准库，简化实现逻辑。我们可以在redis的源码中【sds.h】中看到sds的结构如下:  
```
typedef char *sds;
```
redis3.2分支引入了五种sdshdr类型，目的是为了满足不同长度字符串可以使用不同大小的Header，从而节省内存，每次在创建一个sds时根据sds的实际长度判断应该选择什么类型的sdshdr，不同类型的sdshdr占用的内存空间不同。这样细分一下可以省去很多不必要的内存开销，下面是3.2的sdshdr定义  
```
struct __attribute__ ((__packed__)) sdshdr8 { 8表示字符串最大长度是2^8-1 (长度为255) 
  uint8_t len;//表示当前sds的长度(单位是字节) 
  uint8_t alloc; //表示已为sds分配的内存大小(单位是字节)
  unsigned char flags; //用一个字节表示当前sdshdr的类型，因为有sdshdr有五种类型，所以至少需要3位来表示000:sdshdr5，001:sdshdr8，    010:sdshdr16，011:sdshdr32，100:sdshdr64。高5位用不到所以都为0。 
  char buf[];//sds实际存放的位置
}
```
sdshdr8的内存布局  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/String%E7%BB%93%E6%9E%84.jpeg)  
  

#### 列表类型  
列表类型(list)可以存储一个有序的字符串列表，常用的操作是向列表两端添加元素或者获得列表的某一个片段。  
列表类型内部使用双向链表实现，所以向列表两端添加元素的时间复杂度为O(1), 获取越接近两端的元素速度就越快。这意味着即使是一个有几千万个元素的列表，获取头部或尾部的10条记录也是很快的。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/List%E7%BB%93%E6%9E%84.jpeg)  
  
数据结构  
redis3.2之前，List类型的value对象内部以linkedlist或者ziplist来实现, 当list的元素个数和单个元素的长度比较小的时候，Redis会采用ziplist(压缩列表)来实现来减少内存占用。否则就会采用linkedlist(双向链表)结构。  
redis3.2之后，采用的一种叫quicklist的数据结构来存储list，列表的底层都由quicklist实现。  
这两种存储方式都有优缺点，双向链表在链表两端进行push和pop操作，在插入节点上复杂度比较低，但是内存开销比较大；ziplist存储在一段连续的内存上，所以存储效率很高，但是插入和删除都需要频繁申请和释放内存；  
quicklist仍然是一个双向链表，只是列表的每个节点都是一个ziplist，其实就是linkedlist和ziplist的结合，quicklist 中每个节点ziplist都能够存储多个数据元素，在源码中的文件为【quicklist.c】，在源码第一行中有解释为:A doubly linked list of ziplists意思为一个由ziplist组成的双向链表;
![](https://github.com/YufeizhangRay/image/blob/master/Redis/List%E5%86%85%E9%83%A8%E7%BB%93%E6%9E%84.jpeg)  
  
#### hash类型  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/Hash%E7%BB%93%E6%9E%84.jpeg)  
  
数据结构  
map提供两种结构来存储，一种是hashtable、另一种是前面讲的ziplist，数据量小的时候用ziplist。在redis中，哈希表分为三层(源码地址【dict.h】)，分别是：  
  
dictEntry  
管理一个key-value，同时保留同一个桶中相邻元素的指针，用来维护哈希桶的内部链；  
```
typedef struct dictEntry {
    void *key;
    union { //因为value有多种类型，所以value用了union来存储 void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } 
struct dictEntry *next;//下一个节点的地址，用来处理碰撞，所有分配到同一索引的元素通过next指针 链接起来形成链表key和v都可以保存多种类型的数据
} dictEntry;
```
  
dictht  
实现一个hash表会使用一个buckets存放dictEntry的地址，一般情况下通过hash(key)%len得到的值就是buckets的索引，这个值决定了我们要将此dictEntry节点放入buckets的哪个索引里,这个buckets实际上就是我们说的hash表。dict.h的dictht结构中table存放的就是buckets的地址。  
```
typedef struct dictht {
  dictEntry **table;//buckets的地址
  unsigned long size;//buckets的大小,总保持为 2^n
  unsigned long sizemask;//掩码，用来计算hash值对应的buckets索引 
  unsigned long used;//当前dictht有多少个dictEntry节点
} dictht;
```
  
dict  
dictht实际上就是hash表的核心，但是只有一个dictht还不够，比如rehash、遍历hash等操作。所以redis定义了一个叫dict的结构以支持字典的各种操作，当dictht需要扩容/缩容时，用来管理dictht的迁移。  
比如我们要讲一个数据存储到hash表中，那么会先通过murmur计算key对应的hashcode，然后根据hashcode取 模得到bucket的位置，再插入到链表中。
```
typedef struct dict {
  dictType *type;//dictType里存放的是一堆工具函数的函数指针，
  void *privdata;//保存type中的某些函数需要作为参数的数据
  dictht ht[2];//两个dictht，ht[0]平时用，ht[1] rehash时用
  long rehashidx; //当前rehash到buckets的哪个索引，-1时表示非rehash状态 
  int iterators; //安全迭代器的计数。
} dict;
```
  
#### 集合类型  
集合类型中，每个元素都是不同的，也就是不能有重复数据，同时集合类型中的数据是无序的。集合类型和列表类型的最大的区别是有序性和唯一性。  
集合类型的常用操作是向集合中加入或删除元素、判断某个元素是否存在。由于集合类型在redis内部是使用的值为空的散列表(hash table)，所以这些操作的时间复杂度都是O(1)。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/Set%E7%B1%BB%E5%9E%8B.jpeg)  
  
数据结构  
Set在的底层数据结构以intset或者hashtable来存储。当set中只包含整数型的元素时，采用intset来存储，否则，采用hashtable存储，但是对于set来说，该hashtable的value值用于为NULL，通过key来存储元素。 
  
#### 有序集合  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E6%9C%89%E5%BA%8F%E9%9B%86%E5%90%88%E7%BB%93%E6%9E%84.jpeg)  
  
有序集合类型，顾名思义，和前面讲的集合类型的区别就是多了有序的功能。  
在集合类型的基础上，有序集合类型为集合中的每个元素都关联了一个分数，这使得我们不仅可以完成插入、删除和判断元素是否存在等集合类型支持的操作，还能获得分数最高(或最低)的前N个元素、获得指定分数范围内的元素等与分数有关的操作。虽然集合中每个元素都是不同的，但是他们的分数却可以相同。
  
数据结构  
zset类型的数据结构就比较复杂一点，内部是以ziplist或者skiplist+hashtable来实现，这里面最核心的一个结构就是skiplist，也就是跳跃表。
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E6%9C%89%E5%BA%8F%E9%9B%86%E5%90%88%E5%86%85%E9%83%A8%E7%BB%93%E6%9E%84.jpeg)  
  
level是数据存放的层，相同层数的数据互相关联。level是随机生成的，数据在插入的时候会获得自己的随机层数，这样就类似于实现了数据的分片。查找数据的时候是从level高的层找向level低的层。如图中，寻找25，level4中没有记录，level3中存在记录，只需两次就可以找到，提升了查找和插入的效率。
  
Redis命令大全  
http://redisdoc.com/  
  
### Redis原理分析  
  
#### 过期时间设置  
在Redis中提供了Expire命令设置一个键的过期时间，到期以后Redis会自动删除它。这个在我们实际使用过程中用得非常多。  
EXPIRE命令的使用方法为  
```
EXPIRE key seconds
```
其中seconds 参数表示键的过期时间，单位为秒。EXPIRE 返回值为1表示设置成功，0表示设置失败或者键不存在。  
  
如果向知道一个键还有多久时间被删除，可以使用TTL命令。  
```
TTL key
```
当键不存在时，TTL命令会返回-2，而对于没有给指定键设置过期时间的，通过TTL命令会返回-1。  
  
如果想取消键的过期时间设置(使该键恢复成为永久的)，可以使用PERSIST命令，如果该命令执行成功或者成功清除了过期时间，则返回1。否则返回0(键不存在或者本身就是永久的)。  
  
EXPIRE命令的seconds命令必须是整数，所以最小单位是1秒，如果向要更精确的控制键的过期时间可以使用 PEXPIRE命令。  
PEXPIRE命令的单位是毫秒。即PEXPIRE key 1000与EXPIRE key 1相等。对应的PTTL以毫秒单位获取键的剩余有效时间。  
  
还有一个针对字符串独有的过期时间设置方式
```
setex(String key,int seconds,String value)
```
  
#### 过期删除的原理  
Redis 删除失效主键的方法主要有两种:  

消极方法(passive way)  
在主键被访问时如果发现它已经失效，那么就删除它。  
  
积极方法(active way)  
周期性地从设置了失效时间的主键中选择一部分失效的主键删除。  
  
对于那些从未被查询的key，即便它们已经过期，被动方式也无法清除。因此Redis会周期性地随机测试一些key，已过期的key将会被删掉。Redis每秒会进行10次操作，具体的流程：  
>1.随机测试 20 个带有timeout信息的key。  
2.删除其中已经过期的key。  
3.如果超过25%的key被删除，则重复执行步骤1。  
  
这是一个简单的概率算法(trivial probabilistic algorithm)，基于假设我们随机抽取的key代表了全部的key空间。 
  
#### 发布订阅  
Redis提供了发布订阅功能，可以用于消息的传输，Redis提供了一组命令可以让开发者实现“发布/订阅”模式 (publish/subscribe)，该模式同样可以实现进程间的消息传递。发布/订阅模式包含两种角色，分别是发布者和订阅者。订阅者可以订阅一个或多个频道，而发布者可以向指定的频道发送消息，所有订阅此频道的订阅者都会收到该消息。  
发布者发布消息的命令是PUBLISH，用法是  
```
PUBLISH channel message  
```
比如向channel.1发一条消息:hello  
```
PUBLISH channel.1 “hello”  
```
这样就实现了消息的发送，该命令的返回值表示接收到这条消息的订阅者数量。因为在执行这条命令的时候还没有订阅者订阅该频道，所以返回为0。另外值得注意的是消息发送出去不会持久化，如果发送之前没有订阅者，那么后续再有订阅者订阅该频道，之前的消息就收不到了。  
订阅者订阅消息的命令是  
```
SUBSCRIBE channel [channel ...]  
```
该命令同时可以订阅多个频道，比如订阅channel.1的频道：  
```
SUBSCRIBE channel.1  
```
执行SUBSCRIBE命令后客户端会进入订阅状态。  
  
结构图  
channel分两类，一个是普通channel、另一个是pattern channel(规则匹配)， producer1发布了一条消息 【publish abc hello】,redis server发给abc这个普通channel上的所有订阅者，同时abc也匹配上了pattern channel的名字，所以这条消息也会同时发送给pattern channel *bc上的所有订阅者。  

![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%8F%91%E5%B8%83%E8%AE%A2%E9%98%85%E7%BB%93%E6%9E%84%E5%9B%BE.jpeg)  
  
#### 数据持久化  
Redis支持两种方式的持久化，一种是RDB(Redis DataBase)方式、另一种是AOF(Append-Only-File)方式。前者会根据指定的规则“定时”将内存中的数据存储在硬盘上，而后者在每次执行命令后将命令本身记录下来。两种持久化方式可以单独使用其中一种，也可以将这两种方式结合使用。  
  
RDB方式  
当符合一定条件时，Redis会单独创建(fork)一个子进程来进行持久化，会先将数据写入到一个临时文件中，等到持久化过程都结束了，再用这个临时文件替换上次持久化好的文件。整个过程中，主进程是不进行任何IO操作的，这就确保了极高的性能。如果需要进行大规模数据的恢复，且对于数据恢复的完整性不是非常敏感，那RDB方式要比AOF方式更加的高效。RDB的缺点是最后一次持久化后的数据可能丢失。  
  
--fork的作用是复制一个与当前进程一样的进程。新进程的所有数据(变量、环境变量、程序计数器等)数值都和原进程一致，但是是一个全新的进程，并作为原进程的子进程。
Redis会在以下几种情况下对数据进行快照  
>1.根据配置规则进行自动快照  
>2.用户执行SAVE或者GBSAVE命令  
>3.执行FLUSHALL命令  
>4.执行复制(replication)时  
  
根据配置规则进行自动快照  
Redis允许用户自定义快照条件，当符合快照条件时，Redis会自动执行快照操作。快照的条件可以由用户在配置文件中配置。配置格式如下
```
save 900 1  
save 300 10
save 60 10000
```
第一个参数是时间窗口，第二个是键的个数，也就是说，在第一个时间参数配置范围内被更改的键的个数大于后面的changes时，即符合快照条件。redis默认配置了以上三个规则。每条快照规则占一行，每条规则之间是“或”的关系。 在900秒(15分)内有一个以上的键被更改则进行快照。  
  
用户执行SAVE或BGSAVE命令  
除了让Redis自动进行快照以外，当我们对服务进行重启或者服务器迁移我们需要人工去干预备份。redis提供了两条命令来完成这个任务。  
  
1.save命令  
当执行save命令时，Redis同步做快照操作，在快照执行过程中会阻塞所有来自客户端的请求。当redis内存中的数据较多时，通过该命令将导致Redis较长时间的不响应。所以不建议在生产环境上使用这个命令，而是推荐使用 bgsave命令。
  
2.bgsave命令  
bgsave命令可以在后台异步地进行快照操作，快照的同时服务器还可以继续响应来自客户端的请求。执行BGSAVE后，Redis会立即返回ok表示开始执行快照操作。  
通过LASTSAVE命令可以获取最近一次成功执行快照的时间(自动快照采用的是异步快照操作)。  

执行FLUSHALL命令  
该命令会清除redis在内存中的所有数据。执行该命令后，只要redis中配置的快照规则不为空，也就是save的规则存在，redis就会执行一次快照操作。不管规则是什么样的都会执行。如果没有定义快照规则，就不会执行快照操作。  

执行复制时  
该操作主要是在主从模式下，redis会在复制初始化时进行自动快照。后面单独叙述，这里只需要了解当执行复制操作时，及时没有定义自动快照规则，并且没有手动执行过快照操作，它仍然会生成RDB快照文件。  

AOF方式  
当使用Redis存储非临时数据时，一般需要打开AOF持久化来降低进程终止导致的数据丢失。AOF可以将Redis执行的每一条写命令追加到硬盘文件中，这一过程会降低Redis的性能，但大部分情况下这个影响是能够接受的，另外使用较快的硬盘可以提高AOF的性能。  

开启AOF  
默认情况下Redis没有开启AOF方式的持久化，可以通过appendonly参数启用，在redis.conf 中找到 appendonly yes。
开启AOF持久化后每执行一条会更改Redis中的数据的命令后，Redis就会将该命令写入硬盘中的AOF文件。  
AOF文件的保存位置和RDB文件的位置相同，都是通过dir参数设置的，默认的文件名是apendonly.aof。可以修改redis.conf 中的属性 appendfilename appendonlyh.aof 来修改文件名。  

AOF的实现  
AOF文件以纯文本的形式记录Redis执行的写命令例如开启AOF持久化的情况下执行如下4条命令  
```
set foo 1  
set foo 2  
set foo 3  
get  
```
redis 会将前3条命令写入AOF文件中，通过vim的方式可以看到aof文件中的内容。  
可以发现AOF文件的内容正是Redis发送的原始通信协议的内容，从内容中我们发现Redis只记录了3条命令。然后这时有一个问题是前面2条命令其实是冗余的，因为这两条的执行结果都会被第三条命令覆盖。随着执行的命令越来越多，AOF文件的大小也会越来越大，其实内存中实际的数据可能没有多少，那这样就会造成磁盘空间以及redis数据还原的过程比较长的问题。因此我们希望Redis可以自动优化 AOF文件，就上面这个例子来说，前面两条是可以被删除的。而实际上Redis也考虑到了，可以配置一个条件，每当达到一定条件时Redis就会自动重写AOF文件，这个条件的配置为  
```
auto-aof-rewrite-percentage 
100 auto-aof-rewrite-min-size 64mb。  
```
auto-aof-rewrite-percentage 表示的是当目前的AOF文件大小超过上一次重写时的AOF文件大小的百分之多少时会再次进行重写，如果之前没有重写过，则以启动时AOF文件大小为依据。  
auto-aof-rewrite-min-size 表示限制了允许重写的最小AOF文件大小，通常在AOF文件很小的情况下即使其中有很多冗余的命令我们也并不太关心。  
另外，还可以通过BGREWRITEAOF 命令手动执行AOF，执行完以后冗余的命令已经被删除了。  
在启动时，Redis会逐个执行AOF文件中的命令来将硬盘中的数据载入到内存中，载入的速度相对于RDB会慢一些。  
   
AOF的重写原理   
Redis 可以在 AOF 文件体积变得过大时，自动地在后台对 AOF 进行重写。重写后的新 AOF 文件包含了恢复当前数据集所需的最小命令集合。    
重写的流程是这样，主进程会fork一个子进程出来进行AOF重写，这个重写过程并不是基于原有的aof文件来做的，而是有点类似于快照的方式，全量遍历内存中的数据，然后逐个序列到aof文件中。在fork子进程这个过程中，服务端仍然可以对外提供服务，那这个时候重写的aof文件的数据和会redis内存数据不一致。于是在这个过程中，主进程的数据更新操作，会缓存到aof_rewrite_buf中，也就是单独开辟一块缓存来存储重写期间收到的命令，当子进程重写完以后再把缓存中的数据追加到新的aof文件。  
当所有的数据全部追加到新的aof文件中后，把新的aof文件重命名为，此后所有的操作都会被写入新的aof文件。  
如果在rewrite过程中出现故障，不会影响原来aof文件的正常工作，只有当rewrite完成后才会切换文件。因此这个rewrite过程是比较可靠的。
  
#### 内存回收策略  
Redis中提供了多种内存回收策略，当内存容量不足时，为了保证程序的运行，这时就不得不淘汰内存中的一些对象，释放这些对象占用的空间。  
其中，默认的策略为noeviction策略，当内存使用达到阈值的时候，所有引起申请内存的命令会报错。  
>allkeys-lru:从数据集(server.db[i].dict)中挑选最近最少使用的数据淘汰。  
适用场景：如果我们的应用对缓存的访问都是相对热点的数据，可以选择这个策略。  
allkeys-random:随机移除某个key。  
volatile-random:从已设置过期时间的数据集(server.db[i].expires)中任意选择数据淘汰。  
volatile-lru:从已设置过期时间的数据集(server.db[i].expires)中挑选最近最少使用的数据淘汰。  
volatile-ttl:从已设置过期时间的数据集(server.db[i].expires)中挑选将要过期的数据淘汰。  
  
实际上Redis实现的LRU并不是可靠的LRU，也就是名义上我们使用LRU算法淘汰内存数据，但是实际上被淘汰的键并不一定是真正的最少使用的数据。这里涉及到一个权衡的问题，如果需要在所有的数据中搜索最符合条件的数据，那么一定会增加系统的开销，Redis是单线程的，所以耗时的操作会谨慎一些。为了在一定成本内实现相对的 LRU，早期的Redis版本是基于采样的LRU，也就是放弃了从所有数据中搜索解改为采样空间搜索最优解。Redis3.0 版本之后，Redis作者对于基于采样的LRU进行了一些优化，目的是在一定的成本内让结果更靠近真实的LRU。  

#### 单线程高性能原理    
Redis采用了一种非常简单的做法，单线程来处理来自所有客户端的并发请求，Redis把任务封闭在一个线程中从而避免了线程安全问题。  
Redis为什么是单线程?  
官方的解释是，CPU并不是Redis的瓶颈所在，Redis的瓶颈主要在机器的内存和网络的带宽。  
那么Redis能不能处理高并发请求呢？当然是可以的，至于怎么实现的，我们来具体了解一下。  
【注意并发不等于并行，并发性I/O流，意味着能够让一个计算单元来处理来自多个客户端的流请求。并行性，意味着服务器能够同时执行几个事情，具有多个计算单元】  
  
I/O多路复用  
Redis 是跑在单线程中的，所有的操作都是按照顺序线性执行的，但是由于读写操作等待用户输入或输出都是阻塞的，所以 I/O 操作在一般情况下往往不能直接返回，这会导致某一文件的 I/O 阻塞导致整个进程无法对其它客户提供服务，而 I/O 多路复用就是为了解决这个问题而出现的。  
了解多路复用之前，先简单了解下几种I/O模型  
>(1)同步阻塞IO(Blocking IO):即传统的IO模型。  
(2)同步非阻塞IO(Non-blocking IO):默认创建的socket都是阻塞的，非阻塞IO要求socket被设置为NONBLOCK。  
(3)IO多路复用(IO Multiplexing):即经典的Reactor设计模式，也称为异步阻塞IO，Java中的Selector和Linux中的epoll都是这种模型。  
(4)异步IO(Asynchronous IO):即经典的Proactor设计模式，也称为异步非阻塞IO。  
  
同步和异步、阻塞和非阻塞，到底是什么意思，感觉原理都差不多，我来简单解释一下。  
>同步和异步，指的是用户线程和内核的交互方式。  
阻塞和非阻塞，指用户线程调用内核IO操作的方式是阻塞还是非阻塞。  
  
就像在Java中使用多线程做异步处理的概念，通过多线程去执行一个流程，主线程可以不用等待。而阻塞和非阻塞我们可以理解为假如在同步流程或者异步流程中做IO操作，如果缓冲区数据还没准备好，IO的这个过程会阻塞。  
  
### 在Redis中使用Lua脚本  
  
我们在使用redis的时候，会面临一些问题，比如：  
  
#### 原子性问题
Redis虽然是单一线程的，当时仍然会存在线程安全问题，当然，这个线程安全问题不是来源安于Redis服务器内部。而是Redis作为数据服务器，是提供给多个客户端使用的。多个客户端的操作就相当于同一个进程下的多个线程，如果多个客户端之间没有做好数据的同步策略，就会产生数据不一致的问题。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%8E%9F%E5%AD%90%E6%80%A7%E9%97%AE%E9%A2%98.jpeg)  
  
#### 效率问题  
Redis本身的吞吐量是非常高的，因为它首先是基于内存的数据库。在实际使用过程中，有一个非常重要的因素影响redis的吞吐量，那就是网络。我们在使用redis实现某些特定功能的时候，很可能需要多个命令或者多个数据类型的交互才能完成，那么这种多次网络请求对性能影响比较大。当然redis也做了一些优化，比如提供了pipeline管道操作，但是它有一定的局限性，就是执行的多个命令和响应之间是不存在相互依赖关系的。所以我们需要一种机制能够编写一些具有业务逻辑的命令，减少网络请求。  

#### Lua  
Redis中内嵌了对Lua环境的支持，允许开发者使用Lua语言编写脚本传到Redis中执行，Redis客户端可以使用Lua脚本，直接在服务端原子的执行多个Redis命令。  
使用脚本的好处:  
>1.减少网络开销，在Lua脚本中可以把多个命令放在同一个脚本中运行。  
2.原子操作，redis会将整个脚本作为一个整体执行，中间不会被其他命令插入。换句话说，编写脚本的过程中无需担心会出现竞态条件。  
3.复用性，客户端发送的脚本会永远存储在redis中，这意味着其他客户端可以复用这一脚本来完成同样的逻辑Lua是一个高效的轻量级脚本语言(javascript、shell、sql、python、ruby...)，用标准C语言编写并以源代码形式开放，其设计目的是为了嵌入应用程序中，从而为应用程序提供灵活的扩展和定制功能。  
  
#### Redis与Lua  
先初步的认识一下在redis中如何结合lua来完成一些简单的操作。 
  
在Lua脚本中调用Redis命令  
在Lua脚本中调用Redis命令，可以使用redis.call函数调用。比如我们调用string类型的命令
```
redis.call(‘set’,’hello’,’world’)
local value=redis.call(‘get’,’hello’)
```
redis.call 函数的返回值就是redis命令的执行结果。Redis的5中类型的数据返回的值的类型也都不一样，redis.call函数会将这5种类型的返回值转化对应的Lua的数据类型。

从Lua脚本中获得返回值  
在很多情况下我们都需要脚本可以有返回值，毕竟这个脚本也是一个我们所编写的命令集，我们可以像调用其他 redis内置命令一样调用我们自己写的脚本，所以同样redis会自动将脚本返回值的Lua数据类型转化为Redis的返回 值类型。 在脚本中可以使用return 语句将值返回给redis客户端，通过return语句来执行，如果没有执行return， 默认返回为nil。  
  
EVAL命令的格式是  
```
[EVAL][脚本内容] [key参数的数量][key ...] [arg ...]
```
可以通过key和arg这两个参数向脚本中传递数据，他们的值可以在脚本中分别使用KEYS和ARGV这两个类型的全局变量访问。比如我们通过脚本实现一个set命令，通过在redis客户端中调用，那么执行的语句是  
```
eval "return redis.call('set',KEYS[1],ARGV[1])" 1 lua1 hello
```
lua脚本的内容为: 
```
return redis.call(‘set’,KEYS[1],ARGV[1]) //KEYS和ARGV必须大写 
```
注意:EVAL命令是根据 key参数的数量-也就是上面例子中的1来将后面所有参数分别存入脚本中KEYS和ARGV两个表类型的全局变量。当脚本不需要任何参数时也不能省略这个参数。如果没有参数则为0。
```
eval "return redis.call('get','lua1')" 0 
```
  
EVALSHA命令  
考虑到我们通过eval执行lua脚本，脚本比较长的情况下，每次调用脚本都需要把整个脚本传给redis，比较占用带宽。为了解决这个问题，redis提供了EVALSHA命令允许开发者通过脚本内容的SHA1摘要来执行脚本。该命令的用法和EVAL一样，只不过是将脚本内容替换成脚本内容的SHA1摘要。  
>1.Redis在执行EVAL命令时会计算脚本的SHA1摘要并记录在脚本缓存中。  
2.执行EVALSHA命令时Redis会根据提供的摘要从脚本缓存中查找对应的脚本内容，如果找到了就执行脚本，否则返回“NOSCRIPT No matching script,Please use EVAL”。  
  
通过以下案例来演示EVALSHA命令的效果
```
script load "return redis.call('get','lua1')" 将脚本加入缓存并生成sha1命令
"a5a402e90df3eaeca2ff03d56d99982e05cf6574" 
```
```
evalsha "a5a402e90df3eaeca2ff03d56d99982e05cf6574" 0 
```
我们在调用eval命令之前，先执行evalsha命令，如果提示脚本不存在，则再调用eval命令。  
  
### Redis集群  
  
先来简单了解下redis中提供的集群策略，虽然redis有持久化功能能够保障redis服务器宕机也能恢复并且只有少量的数据损失，但是由于所有数据在一台服务器上，如果这台服务器出现硬盘故障，那就算是有备份也仍然不可避免数据丢失的问题。  
在实际生产环境中，我们不可能只使用一台redis服务器作为我们的缓存服务器，必须要多台实现集群，避免出现单点故障；  

#### 主从复制  
复制的作用是把redis的数据库复制多个副本部署在不同的服务器上，如果其中一台服务器出现故障，也能快速迁移到其他服务器上提供服务。复制功能可以实现当一台redis服务器的数据更新后，自动将新的数据同步到其他服务器上。  
主从复制就是我们常见的master/slave模式，主数据库可以进行读写操作，当写操作导致数据发生变化时会自动将数据同步给从数据库。而一般情况下，从数据库是只读的，并接收主数据库同步过来的数据。一个主数据库可以有多个从数据库。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E4%B8%BB%E4%BB%8E.jpeg)  
  
配置  
在redis中配置master/slave是非常容易的，只需要在从数据库的配置文件中加入slaveof 主数据库地址端口。 而 master 数据库不需要做任何改变。
准备两台服务器server1和server2，分别安装redis。  
>1.在server2的redis.conf文件中增加 slaveof server1-ip 6379，同时将bindip注释掉，允许所有ip访问。  
2.启动server2。  
3.访问server2的redis客户端，输入 INFO replication。  
4.通过在master机器上输入命令，比如set name zyf，在slave服务器就能看到该值已经同步过来了。  
 
#### 主从复制原理  

全量复制  
Redis全量复制一般发生在Slave初始化阶段，这时Slave需要将Master上的所有数据都复制一份。具体步骤  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%85%A8%E9%87%8F%E5%A4%8D%E5%88%B6.jpeg)  
  
完成上面几个步骤后就完成了slave服务器数据初始化的所有操作，savle服务器此时可以接收来自用户的读请求。  
master/slave 复制策略是采用乐观复制，也就是说可以容忍在一定时间内master/slave数据的内容是不同的，但是两者的数据会最终同步。  
具体来说，redis的主从同步过程本身是异步的，意味着master执行完客户端请求的命令后会立即返回结果给客户端，然后异步的方式把命令同步给slave。  
这一特征保证启用master/slave后master的性能不会受到影响。  
但是另一方面，如果在这个数据不一致的窗口期间，master/slave因为网络问题断开连接，而这个时候，master 是无法得知某个命令最终同步给了多少个slave数据库。于是redis提供了一个配置项来限制只有数据至少同步给多少个slave的时候，master才是可写的。  
>min-slaves-to-write 3 表示只有当3个或以上的slave连接到master，master才是可写的。  
min-slaves-max-lag 10 表示允许slave最长失去连接的时间，如果10秒还没收到slave的响应，则master认为该slave已断开。  
  
增量复制  
从redis 2.8开始，就支持主从复制的断点续传，如果主从复制过程中，网络连接断掉了，那么可以接着上次复制的地方，继续复制下去，而不是从头开始复制一份。  
master node会在内存中创建一个backlog，master和slave都会保存一个replica offset还有一个master id，offset 就是保存在backlog中的。如果master和slave网络连接断掉了，slave会让master从上次的replica offset开始继续复制。
但是如果没有找到对应的offset，那么就会执行一次全量同步。  
  
无硬盘复制  
前面我们说过，Redis复制的工作原理基于RDB方式的持久化实现的，也就是master在后台保存RDB快照，slave接收到rdb文件并载入，但是这种方式会存在一些问题。  
>1.当master禁用RDB时，如果执行了复制初始化操作，Redis依然会生成RDB快照，当master下次启动时执行该RDB文件的恢复，但是因为复制发生的时间点不确定，所以恢复的数据可能是任何时间点的，就会造成数据出现问题。  
2.当硬盘性能比较慢的情况下(网络硬盘)，那初始化复制过程会对性能产生影响。  
  
因此2.8.18以后的版本，Redis引入了无硬盘复制选项，可以不需要通过RDB文件去同步，直接发送数据，通过以下配置来开启该功能  
```
repl-diskless-sync yes
master** rdb slave
```
  
#### 哨兵机制  
在前面讲的master/slave模式，在一个典型的一主多从的系统中，slave在整个体系中起到了数据冗余备份和读写分离的作用。当master遇到异常终端后，需要从slave中选举一个新的master继续对外提供服务，这种机制应用非常广泛，比如在zk中通过leader选举、kafka中可以基于zk的节点实现master选举。所以在redis中也需要一种机制去实现master的决策，redis并没有提供自动master选举功能，而是需要借助一个哨兵来进行监控。  
  
什么是哨兵  
顾名思义，哨兵的作用就是监控Redis系统的运行状况，它的功能包括两个：
>1.监控master和slave是否正常运行。  
2.master出现故障时自动将slave数据库升级为master。  
  
哨兵是一个独立的进程，使用哨兵后的架构图  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%93%A8%E5%85%B5.jpeg)  
  
为了解决master选举问题，又引出了一个单点问题，也就是哨兵的可用性如何解决，在一个一主多从的Redis系统中，可以使用多个哨兵进行监控任务以保证系统足够稳定。此时哨兵不仅会监控master和slave，同时还会互相监控。这种方式称为哨兵集群，哨兵集群需要解决故障发现、和master决策的协商机制问题。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%93%A8%E5%85%B5%E9%9B%86%E7%BE%A4.jpeg)  
  
sentinel之间的相互感知  
sentinel节点之间会因为共同监视同一个master从而产生了关联，一个新加入的sentinel节点需要和其他监视相同master节点的sentinel相互感知。  
>1.需要相互感知的sentinel都向他们共同监视的master节点订阅channel:sentinel:hello。  
2.新加入的sentinel节点向这个channel发布一条消息，包含自己本身的信息，这样订阅了这个channel的sentinel 就可以发现这个新的sentinel。  
3.新加入得sentinel和其他sentinel节点建立长连接。  
  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%93%A8%E5%85%B5%E6%84%9F%E7%9F%A5.jpeg)  
  
master的故障发现  
sentinel节点会定期向master节点发送心跳包来判断存活状态，一旦master节点没有正确响应，sentinel会把 master设置为“主观不可用状态”，然后它会把“主观不可用”发送给其他所有的sentinel节点去确认，当确认的sentinel节点数大于>quorum时，则会认为master是“客观不可用”，接着就开始进入选举新的master流程。但是这里又会遇到一个问题，就是sentinel中，本身是一个集群，如果多个节点同时发现master节点达到客观不可用状态，那谁来决策选择哪个节点作为maste呢？这个时候就需要从sentinel集群中选择一个leader来做决策。这里用到了Raft算法，这是一种分布式一致性算法，基于投票，只要保证过半数节点通过提议即可；  
  
动画演示地址:http://thesecretlivesofdata.com/raft/  
  
配置实现  
通过在这个配置的基础上增加哨兵机制。在其中任意一台服务器上创建一个sentinel.conf文件，文件内容：  
```
sentinel monitor name ip port quorum    
```
其中name表示要监控的master的名字，这个名字是自己定义。ip和port表示master的ip和端口号。最后一个1表示最低通过票数，也就是说至少需要几个哨兵节点同意才可以。
```
port 6040
sentinel monitor mymaster 192.168.11.131 6379 1
sentinel down-after-milliseconds mymaster 5000 --表示如果5s内mymaster没响应，就认为SDOWN
sentinel failover-timeout mymaster 15000 --表示如果15秒后,mysater仍没活过来，则启动failover，从剩下的slave中选一个升级为master
```
  
两种方式启动哨兵  
```
redis-sentinel sentinel.conf
redis-server /path/to/sentinel.conf --sentinel
```

哨兵监控一个系统时，只需要配置监控master即可，哨兵会自动发现所有slave。  
这时候，我们把master关闭，等待指定时间后(默认是30秒)，会自动进行切换，会输出如下消息：   
```
+sdown表示哨兵主管认为master已经停止服务了。
+odown表示哨兵客观认为master停止服务了。
+try-failover表示哨兵开始进行故障恢复。 
+failover-end 表示哨兵完成故障恢复。
+slave表示列出新的master和slave服务器，我们仍然可以看到已经停掉的master，哨兵并没有清楚已停止的服务的实例，这是因为已经停止的服务器有可能会在某个时间进行恢复，恢复以后会以slave角色加入到整个集群中。
```
  
#### Redis-Cluster  
即使是使用哨兵，此时的Redis集群的每个数据库依然存有集群中的所有数据，从而导致集群的总数据存储量受限于可用存储内存最小的节点，形成了木桶效应。而因为Redis是基于内存存储的，所以这一个问题在redis中就显得尤为突出了。  
在redis3.0之前，我们是通过在客户端去做的分片，通过hash环的方式对key进行分片存储。分片虽然能够解决各个节点的存储压力，但是导致维护成本高、增加、移除节点比较繁琐。因此在redis3.0以后的版本最大的一个好处就是支持集群功能，集群的特点在于拥有和单机实例一样的性能，同时在网络分区以后能够提供一定的可访问性以 及对主数据库故障恢复的支持。  
哨兵和集群是两个独立的功能，当不需要对数据进行分片使用哨兵就够了，如果要进行水平扩容，集群是一个比较好的方式。  
  
拓扑结构  
一个Redis Cluster由多个Redis节点构成。不同节点组服务的数据没有交集，也就是每个一节点组对应数据 sharding的一个分片。节点组内部分为主备两类节点，对应master和slave节点。两者数据准实时一致，通过异步化的主备复制机制来保证。一个节点组有且只有一个master节点，同时可以有0到多个slave节点，在这个节点组中 只有master节点对用户提供些服务，读服务可以由master或者slave提供。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/cluster.jpeg)  
  
redis-cluster是基于gossip协议实现的无中心化节点的集群，因为去中心化的架构不存在统一的配置中心，各个节点对整个集群状态的认知来自于节点之间的信息交互。在Redis Cluster，这个信息交互是通过Redis Cluster Bus来完成的。  

Redis的数据分区  
分布式数据库首要解决把整个数据集按照分区规则映射到多个节点的问题，即把数据集划分到多个节点上，每个节点负责整个数据的一个子集。Redis Cluster采用哈希分区规则，采用虚拟槽分区。  
虚拟槽分区巧妙地使用了哈希空间，使用分散度良好的哈希函数把所有的数据映射到一个固定范围内的整数集合，整数定义为槽(slot)。比如Redis Cluster槽的范围是0 ~ 16383。槽是集群内数据管理和迁移的基本单位。采用大范围的槽的主要目的是为了方便数据的拆分和集群的扩展，每个节点负责一定数量的槽。  
计算公式:slot = CRC16(key)%16383。每一个节点负责维护一部分槽以及槽所映射的键值数据。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%88%86%E7%89%87.jpeg)  
  
HashTags  
通过分片手段，可以将数据合理的划分到不同的节点上，这本来是一件好事。但是有的时候，我们希望对相关联的业务以原子方式进行操作。举个简单的例子：  
我们在单节点上执行MSET，它是一个原子性的操作，所有给定的key会在同一时间内被设置，不可能出现某些指定的key被更新另一些指定的key没有改变的情况。但是在集群环境下，我们仍然可以执行MSET命令，但它的操作不在是原子操作，会存在某些指定的key被更新，而另外一些指定的key没有改变，原因是多个key可能会被分配到不 同的机器上。  
所以，这里就会存在一个矛盾点，既要求key尽可能的分散在不同机器，又要求某些相关联的key分配到相同机器。  
从前面的分析中我们了解到，分片其实就是一个hash的过程，对key做hash取模然后划分到不同的机器上。所以为了解决这个问题，我们需要考虑如何让相关联的key得到的hash值都相同。如果key全部相同是不现实的，所以redis中引入了HashTag的概念，可以使得数据分布算法可以根据key的某一个部分进行计算，然后让相关的key落到同一个数据分片。  
举个简单的例子，加入对于用户的信息进行存储，
```
user:user1:id
user:user1:name
```
那么通过hashtag的方式， 
```
user:{user1}:id
user:{user1}.name; 
```
表示当一个key包含 {} 的时候，就不对整个key做hash，而仅对 {} 包括的字符串做hash。  
  
重定向客户端  
Redis Cluster并不会代理查询，那么如果客户端访问了一个key并不存在的节点，这个节点是怎么处理的呢？比如我想获取key为msg的值，msg计算出来的槽编号为254，当前节点正好不负责编号为254的槽，那么就会返回客户端下面信息:
```
-MOVED 254 127.0.0.1:6381 
```
表示客户端想要的254槽由运行在IP为127.0.0.1，端口为6381的Master实例服务。如果根据key计算得出的槽恰好由当前节点负责，则当期节点会立即返回结果。  

分片迁移  
在一个稳定的Redis cluster下，每一个slot对应的节点是确定的，但是在某些情况下，节点和分片对应的关系会发生变更。  
>1.新加入master节点  
2.某个节点宕机  
  
也就是说当动态添加或减少node节点时，需要将16384个槽做个再分配，槽中的键值也要迁移。当然，这一过程，在目前实现中，还处于半自动状态，需要人工介入。  

新增一个主节点  
新增一个节点D，redis cluster的这种做法是从各个节点的前面各拿取一部分slot到D上。大致就会变成这样:  
>节点A覆盖1365-5460  
节点B覆盖6827-10922  
节点C覆盖12288-16383  
节点D覆盖0-1364,5461-6826,10923-12287  
  
删除一个主节点  
先将节点的数据移动到其他节点上，然后才能执行删除。  
  
槽迁移的过程  
槽迁移的过程中有一个不稳定状态，这个不稳定状态会有一些规则，这些规则定义客户端的行为，从而使得Redis Cluster不必宕机的情况下可以执行槽的迁移。下面这张图描述了我们迁移编号为1、2、3的槽的过程中，他们在 MasterA节点和MasterB节点中的状态。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E8%BF%81%E7%A7%BB.jpeg)  
  
简单的工作流程  
>1.向MasterB发送状态变更命令，把MasterB对应的slot状态设置为IMPORTING。  
2.向MasterA发送状态变更命令，将Master对应的slot状态设置为MIGRATING。 
  
当MasterA的状态设置为MIGRANTING后，表示对应的slot正在迁移，为了保证slot数据的一致性，MasterA此时对于slot内部数据提供读写服务的行为和通常状态下是有区别的。
  
MIGRATING状态   
>1.如果客户端访问的Key还没有迁移出去，则正常处理这个key。  
2.如果key已经迁移或者根本就不存在这个key，则回复客户端ASK信息让它跳转到MasterB去执行。  
  
IMPORTING状态  
当MasterB的状态设置为IMPORTING后，表示对应的slot正在向MasterB迁入，虽然MasterB仍然能对外提供该slot的读写服务，但和通常状态下也是有区别的。  
当来自客户端的正常访问不是从ASK跳转过来的，说明客户端还不知道迁移正在进行，很有可能操作了一个目前还没迁移完成的并且还存在于MasterA上的key，如果此时这个key在A上已经被修改了，那么B和A的修改则会发生冲突。所以对于MasterB上的slot上的所有非ASK跳转过来的操作，MasterB都不会去护理，而是通过MOVED命令让客户端跳转到MasterA上去执行。  
这样的状态控制保证了同一个key在迁移之前总是在源节点上执行，迁移后总是在目标节点上执行，防止出现两边同时写导致的冲突问题。而且迁移过程中新增的key一定会在目标节点上执行，源节点也不会新增key，是的整个迁移过程既能对外正常提供服务，又能在一定的时间点完成slot的迁移。 
  
### Redis Java API 操作方法及原理分析  
  
已有的客户端支持  
Redis Java客户端有很多的开源产品比如Redission、Jedis、lettuce。  
  
差异  
Jedis是Redis的Java实现的客户端，其API提供了比较全面的Redis命令的支持。  
Redisson实现了分布式和可扩展的Java数据结构，和Jedis相比，功能较为简单，不支持字符串操作，不支持排序、事务、管道、分区等Redis特性。Redisson主要是促进使用者对Redis的关注分离，从而让使用者能够将精力更集中地放在处理业务逻辑上。    
lettuce是基于Netty构建的一个可伸缩的线程安全的Redis客户端，支持同步、异步、响应式模式。多个线程可以共享一个连接实例，而不必担心多线程并发问题。  

#### Jedis-sentinel原理分析  
客户端通过连接到哨兵集群，通过发送Protocol.SENTINEL_GET_MASTER_ADDR_BY_NAME 命令，从哨兵机器中 询问master节点的信息，拿到master节点的ip和端口号以后，再到客户端发起连接。连接以后，需要在客户端建立监听机制，当master重新选举之后，客户端需要重新连接到新的master节点。  
源码分析   
```
private HostAndPort initSentinels(Set<String> sentinels, final String masterName) {
  HostAndPort master = null;
  boolean sentinelAvailable = false;
  log.info("Trying to find master from available Sentinels..."); // 有多个sentinels,遍历这些个sentinels
  for (String sentinel : sentinels) {
  // host:port表示的sentinel地址转化为一个HostAndPort对象。
    final HostAndPort hap = HostAndPort.parseString(sentinel);
    log.fine("Connecting to Sentinel " + hap);
    Jedis jedis = null;
    try {
      // 连接到sentinel
      jedis = new Jedis(hap.getHost(), hap.getPort());
      // 根据masterName得到master的地址，返回一个list，host= list[0], port =// list[1] 
      List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName); // connected to sentinel...
      sentinelAvailable = true;
      if (masterAddr == null || masterAddr.size() != 2) {
        log.warning("Can not get master addr, master name: " + masterName + ". Sentinel: " + hap + ".");
        continue; 
      }
      // 如果在任何一个sentinel中找到了master，不再遍历sentinels 
      master = toHostAndPort(masterAddr); 
      log.fine("Found Redis master at " + master); 
      break;
    } catch (JedisException e) {
      // resolves #1036, it should handle JedisException there's another chance
      // of raising JedisDataException
      log.warning("Cannot get master address from sentinel running @ " + hap + ". Reason: " + e + ". Trying next one.");
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
  }
  // 到这里，如果master为null，则说明有两种情况，一种是所有的sentinels节点都down掉了，一种是master节 点没有被存活的sentinels监控到
  if (master == null) {
    if (sentinelAvailable) {
      // can connect to sentinel, but master name seems to not
      // monitored
      throw new JedisException("Can connect to sentinel, but " + masterName + " seems to be not monitored...");
    } else {
      throw new JedisConnectionException("All sentinels down, cannot determine where is + masterName + " master is running...");
    } 
  }
  //如果走到这里，说明找到了master的地址
  log.info("Redis master running at " + master + ", starting Sentinel listeners...");
  //启动对每个sentinels的监听 为每个sentinel都启动了一个监听者MasterListener。MasterListener本身是一个线程，它会去订阅sentinel 上关于master节点地址改变的消息。
  for (String sentinel : sentinels) {
    final HostAndPort hap = HostAndPort.parseString(sentinel);
    MasterListener masterListener = new MasterListener(masterName, hap.getHost(), hap.getPort());
    // whether MasterListener threads are alive or not, process can be stopped
    masterListener.setDaemon(true);
    masterListeners.add(masterListener);
    masterListener.start();
  }
  return master;
}
```
从哨兵节点获取master信息的方法
```
public List<String> sentinelGetMasterAddrByName(String masterName) {
  client.sentinel(Protocol.SENTINEL_GET_MASTER_ADDR_BY_NAME, masterName);
  final List<Object> reply = client.getObjectMultiBulkReply();
  return BuilderFactory.STRING_LIST.build(reply);
}
```
  
#### Jedis-cluster原理分析 
连接方式  
```
Set<HostAndPort> hostAndPorts=new HashSet<>();
HostAndPort hostAndPort=new HostAndPort("192.168.11.153",7000);
HostAndPort hostAndPort1=new HostAndPort("192.168.11.153",7001);
HostAndPort hostAndPort2=new HostAndPort("192.168.11.154",7003);
HostAndPort hostAndPort3=new HostAndPort("192.168.11.157",7006);
hostAndPorts.add(hostAndPort);
hostAndPorts.add(hostAndPort1);
hostAndPorts.add(hostAndPort2);
hostAndPorts.add(hostAndPort3);
JedisCluster jedisCluster=new JedisCluster(hostAndPorts,6000);
jedisCluster.set("mic","hello");
```
  
原理分析  
程序启动初始化集群环境  
>1.读取配置文件中的节点配置，无论是主从，无论多少个，只拿第一个，获取redis连接实例。  
2.用获取的redis连接实例执行clusterNodes()方法，实际执行redis服务端cluster nodes命令，获取主从配置信息。  
3.解析主从配置信息，先把所有节点存放到nodes的map集合中，key为节点的ip:port，value为当前节点的 jedisPool。  
4.解析主节点分配的slots区间段，把slot对应的索引值作为key，第三步中拿到的jedisPool作为value，存储在slots的map集合中。  
   
这样就实现了slot槽索引值与jedisPool的映射，这个jedisPool包含了master的节点信息，所以槽和几点是对应的，与redis服务端一致。  
  
从集群环境存取值  
>1.把key作为参数，执行CRC16算法，获取key对应的slot值。   
2.通过该slot值，去slots的map集合中获取jedisPool实例。  
3.通过jedisPool实例获取jedis实例，最终完成redis数据存取工作。  
  
#### Redisson的操作方式 
redis-cluster连接方式  
```
Config config=new Config();
config.useClusterServers().setScanInterval(2000)
          .addNodeAddress("redis://192.168.11.153:7000",
                "redis://192.168.11.153:7001",
                "redis://192.168.11.154:7003","redis://192.168.11.157:7006");
RedissonClient redissonClient= Redisson.create(config);
RBucket<String> rBucket=redissonClient.getBucket("mic");
System.out.println(rBucket.get());
```
常规操作命令  
```
 getBucket-> 获取字符串对象
 getMap -> 获取map对象 
 getSortedSet->获取有序集合 
 getSet -> 获取集合
 getList ->获取列表
```
  
### Redis实战及源码分析 

关于锁，其实我们或多或少都有接触过一些，比如synchronized、 Lock这些。这类锁的目的很简单，在多线程环境下，对共享资源的访问造成的线程安全问题，通过锁的机制来实现资源访问互斥。那么什么是分布式锁呢？或者为什么我们需要通过Redis来构建分布式锁，其实最根本原因就是Score(范围)，因为在分布式架构中，所有的应 用都是进程隔离的，在多进程访问共享资源的时候我们需要满足互斥性，就需要设定一个所有进程都能看得到的范围，而这个范围就是Redis本身。所以我们才需要把锁构建到Redis中。  
Redis里面提供了一些比较具有能够实现锁特性的命令，比如SETEX(在键不存在的情况下为键设置值)，那么我们可以基于这个命令来去实现一些简单的锁的操作。  
  
#### 分布式锁实战  
Redisson实现分布式锁  
Redisson它除了常规的操作命令以外，还基于redis本身的特性去实现了很多功能的封装，比如分布式锁、原子操作、布隆过滤器、队列等等。我们可以直接利用这个api提供的功能去实现。
```
Config config=new Config();
config.useSingleServer().setAddress("redis://192.168.11.152:6379");
RedissonClient redissonClient=Redisson.create(config);
RLock rLock=redissonClient.getLock("updateOrder"); //最多等待100秒、上锁10s以后自动解锁 
if(rLock.tryLock(100,10,TimeUnit.SECONDS)){
  System.out.println("获取锁成功"); 
}
```
  
原理分析  
trylock  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/tryLock.jpeg)  
  
tryAcquireAsync  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/tryAcquire.jpeg)  
  
tryLockInnerAsync  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/tryLockInnerAsync.jpeg)  
  
通过lua脚本来实现加锁的操作  
>1.判断lock键是否存在，不存在直接调用hset存储当前线程信息并且设置过期时间，返回nil，告诉客户端直接获取到锁。  
2.判断lock键是否存在，存在则将重入次数加1，并重新设置过期时间，返回nil，告诉客户端直接获取到锁。  
3.被其它线程已经锁定，返回锁有效期的剩余时间，告诉客户端需要等待。  
  
unlock
![](https://github.com/YufeizhangRay/image/blob/master/Redis/unLock.jpeg)  
  
1.如果lock键不存在，发消息说锁已经可用，发送一个消息。  
2.如果锁不是被当前线程锁定，则返回nil。  
3.由于支持可重入，在解锁时将重入次数需要减1。  
4.如果计算后的重入次数>0，则重新设置过期时间。  
5.如果计算后的重入次数<=0，则发消息说锁已经可用。  
  
#### 管道模式  
Redis服务是一种C/S模型，提供请求-响应式协议的TCP服务，所以当客户端发起请求，服务端处理并返回结果到客户端，一般是以阻塞形式等待服务端的响应，但这在批量处理连接时延迟问题比较严重，所以Redis为了提升或弥补这个问题，引入了管道技术：可以做到服务端未及时响应的时候，客户端也可以继续发送命令请求，做到客户端和服务端互不影响，服务端并最终返回所有服务端的响应，大大提高了C/S模型交互的响应速度上有了质的提高。  
  
使用方法  
```
Jedis jedis=new Jedis("192.168.11.152",6379);
Pipeline pipeline=jedis.pipelined();
for(int i=0;i<1000;i++){
    pipeline.incr("test");
}
pipeline.sync();
```
  
### Redis的应用架构  
对于读多写少的高并发场景，我们会经常使用缓存来进行优化。比如说支付宝的余额展示功能，实际上99%的时候都是查询，1%的请求是变更。所以，我们在这样的场景下，可以加入缓存。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%BA%94%E7%94%A8%E6%9E%B6%E6%9E%84.jpeg)  
  
#### 缓存与数据一致性问题  
那么基于上面的这个出发点，问题就来了，当用户的余额发生变化的时候，如何更新缓存中的数据，也就是说。  
>1.我是先更新缓存中的数据再更新数据库的数据。  
2.还是修改数据库中的数据再更新缓存中的数据。  
  
数据库的数据和缓存中的数据如何达到一致性？首先，可以肯定的是，redis中的数据和数据库中的数据不可能保证事务性达到统一的，这个是毫无疑问的，所以在实际应用中，我们都是基于当前的场景进行权衡降低出现不一致问题的出现概率。  
  
更新缓存还是让缓存失效  
更新缓存表示数据不但会写入到数据库，还会同步更新缓存。而让缓存失效是表示只更新数据库中的数据，然后删除缓存中对应的key。那么这两种方式怎么去选择？这块有一个衡量的指标。  
>1.如果更新缓存的代价很小，那么可以先更新缓存，这个代价很小的意思是我不需要很复杂的计算去获得最新的余额数字。  
2.如果是更新缓存的代价很大，意味着需要通过多个接口调用和数据查询才能获得最新的结果，那么可以先淘汰缓存。淘汰缓存以后后续的请求如果在缓存中找不到，自然去数据库中检索。  
  
先操作数据库还是先操作缓存  
当客户端发起事务类型请求时，假设我们以让缓存失效作为缓存的的处理方式，那么又会存在两个情况：
>1.先更新数据库再让缓存失效。  
2.先让缓存失效，再更新数据库。  
  
更新数据库和更新缓存这两个操作，是无法保证原子性的，所以我们需要根据当前业务的场景的容忍性来选择。也就是如果出现不一致的情况下，哪一种更新方式对业务的影响最小，就先执行影响最小的方案。
  
假设我们选择先更新数据库，后让缓存失效。  
我们可以加入一个消息中间件，若缓存失效操作失败，则把失败记录传入MQ，让应用消费此消息，再次尝试缓存失效操作。  
最终一致性的解决方案  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E7%BC%93%E5%AD%98%E4%B8%80%E8%87%B4.jpeg)  
  
#### 缓存雪崩与缓存穿透  
当缓存大规模渗透在整个架构中以后，那么缓存本身的可用性讲决定整个架构的稳定性。  
缓存在应用过程中可能会导致的问题。  
  
缓存雪崩  
缓存雪崩是指设置缓存时采用了相同的过期时间，导致缓存在某一个时刻同时失效，或者缓存服务器宕机宕机导致缓存全面失效，请求全部转发到了DB层面，DB由于瞬间压力增大而导致崩溃。缓存失效导致的雪崩效应对底层系统的冲击是很大的。  
  
解决方式  
>1.对缓存的访问，如果发现从缓存中取不到值，那么通过加锁或者队列的方式保证缓存的单进程操作，从而避免失效时并发请求全部落到底层的存储系统上。但是这种方式会带来性能上的损耗。  
2.将缓存失效的时间分散，降低每一个缓存过期时间的重复率。  
3.如果是因为缓存服务器故障导致的问题，一方面需要保证缓存服务器的高可用。另一方面，应用程序中可以采用多级缓存。  
  
缓存穿透  
缓存穿透是指查询一个根本不存在的数据，缓存和数据源都不会命中。出于容错的考虑，如果从数据层查不到数据则不写入缓存，即数据源返回值为 null 时，不缓存 null。缓存穿透问题可能会使后端数据源负载加大，由于很多后端数据源不具备高并发性，甚至可能造成后端数据源宕掉。  
  
解决方式  
>1.如果查询数据库也为空，直接设置一个默认值存放到缓存，这样第二次到缓冲中获取就有值了，而不会继续访问数据库，这种办法最简单粗暴。比如，”key”，“&&”。
在返回这个&&值的时候，我们的应用就可以认为这是不存在的key，那我们的应用就可以决定是否继续等待继续访问，还是放弃掉这次操作。如果继续等待访问，过一个时间轮询点后，再次请求这个key，如果取到的值不再是&&，则可以认为这时候key有值了，从而避免了透传到数据库，从而把大量的类似请求挡在了缓存之中。  
2.根据缓存数据Key的设计规则，将不符合规则的key进行过滤采用布隆过滤器，将所有可能存在的数据哈希到一个足够大的BitSet中，不存在的数据将会被拦截掉，从而避免了对底层存储系统的查询压力。  
  
布隆过滤器  
布隆过滤器是Burton Howard Bloom在1970年提出来的，一种空间效率极高的概率型算法和数据结构，主要用来判断一个元素是否在集合中存在。因为他是一个概率型的算法，所以会存在一定的误差，如果传入一个值去布隆过滤器中检索，可能会出现检测存在的结果但是实际上可能是不存在的，但是肯定不会出现实际上不存在然后反馈存 在的结果。因此，Bloom Filter不适合那些“零错误”的应用场合。而在能容忍低错误率的应用场合下，Bloom Filter 通过极少的错误换取了存储空间的极大节省。  
  
bitmap  
所谓的Bit-map就是用一个bit位来标记某个元素对应的Value，通过Bit为单位来存储数据，可以大大节省存储空间。所以我们可以通过一个int型的整数的32比特位来存储32个10进制的数字，那么这样所带来的好处是内存占用少、效率很高(不需要比较和位移)比如我们要存储5(101)、3(11)四个数字，那么我们申请int型的内存空间，会有32 个比特位。这四个数字的二进制分别对应从右往左开始数，比如第一个数字是5，对应的二进制数据是101, 那么从右往左数到第5位，把对应的二进制数据 存储到32个比特位上。  
```
第一个5就是 00000000000000000000000000101000   
再输入3时候 00000000000000000000000000001100  
```
  
布隆过滤器原理  
有了对位图的理解以后，我们对布隆过滤器的原理理解就会更容易了，仍然以前面提到的40亿数据为案例，假设这40亿数据为某邮件服务器的黑名单数据，邮件服务需要根据邮箱地址来判断当前邮箱是否属于垃圾邮件。原理如下  
假设集合里面有3个元素{x, y, z}，哈希函数的个数为3。首先将位数组进行初始化，将里面每个位都设置位0。对于 集合里面的每一个元素，将元素依次通过3个哈希函数进行映射，每次映射都会产生一个哈希值，这个值对应位数 组上面的一个点，然后将位数组对应的位置标记为1。查询W元素是否存在集合中的时候，同样的方法将W通过哈希映射到位数组上的3个点。如果3个点的其中有一个点不为1，则可以判断该元素一定不存在集合中。反之，如果 3个点都为1，则该元素可能存在集合中。
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8%E5%8E%9F%E7%90%86.jpeg)  
  
接下来按照该方法处理所有的输入对象，每个对象都可能把bitMap中一些白位置涂黑，也可能会遇到已经涂黑的位置，遇到已经为黑的让他继续为黑即可。处理完所有的输入对象之后，在bitMap中可能已经有相当多的位置已经被涂黑。至此，一个布隆过滤器生成完成，这个布隆过滤器代表之前所有输入对象组成的集合。  
如何去判断一个元素是否存在bit array中呢? 原理是一样，根据k个哈希函数去得到的结果，如果所有的结果都是 1，表示这个元素可能(假设某个元素通过映射对应下标为4，5，6这3个点。虽然这3个点都为1，但是很明显这3个点是不同元素经过哈希得到的位置，因此这种情况说明元素虽然不在集合中，也可能对应的都是1)存在。如果一旦发现其中一个比特位的元素是0，表示这个元素一定不存在。  
至于k个哈希函数的取值为多少，能够最大化的降低错误率(因为哈希函数越多，映射冲突会越少)，这个地方就 会涉及到最优的哈希函数个数的一个算法逻辑。  
![](https://github.com/YufeizhangRay/image/blob/master/Redis/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8%E5%BA%94%E7%94%A8.jpeg)  
