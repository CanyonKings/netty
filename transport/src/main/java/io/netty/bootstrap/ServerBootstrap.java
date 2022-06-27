/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link Bootstrap} sub-class which allows easy bootstrap of {@link ServerChannel}
 *
 */
//todo ServerBootstrap 是一个辅助类,允许我们很轻松的启动一个服务器端的通道
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);

    // The order in which child ChannelOptions are applied is important they may depend on each other for validation
    // purposes.
    //todo 下面是关于协议的配置项信息的封装
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    //todo 下面是运行时用户存储进去的数据的封装容器
    private final Map<AttributeKey<?>, Object> childAttrs = new ConcurrentHashMap<AttributeKey<?>, Object>();

    // 配置
    //todo 创建ServerBootstraptConfig
    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);

    //todo volatile确保编译时不会出现指令的重排序
    //todo 明确 happen 和 before 的关系
    private volatile EventLoopGroup childGroup;

    //todo 服务于 childGroup, 处理这个循环组中请求
    private volatile ChannelHandler childHandler;

    public ServerBootstrap() { }

    /**
     * 克隆bootstrap 使用本构造方法
     * @param bootstrap
     */
    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        //todo  从NioServerBootstrapt角度来看,  实际上并没有执行这个构造函数,去初始化下面的childGroup  -- 处理IO事件的事件循环组
        //todo 而是在下面的大约100行的地方初始化的, 下面去看100行
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        childAttrs.putAll(bootstrap.childAttrs);
    }

    /**
     * Specify the {@link EventLoopGroup} which is used for the parent (acceptor) and the child (client).
     */
    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These
     * {@link EventLoopGroup}'s are used to handle all the events and IO for {@link ServerChannel} and
     * {@link Channel}'s.
     */
    //todo 初始化接受请求的 parentGroup 和处理请求的 childGroup,  这两个事件循环组,被用于 处理所有的 ServerChannel 和 Channel
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        //todo 调用父类AbstractBootstrap 的构造, 传递进去 parentGroup ， 把parentGroup 交给他的父类， 他的父类用他管理全部的 将被创建Channel
        super.group(parentGroup);
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        //todo 调用父类AbstractBootstrap 的构造, 传递进去 parentGroup ， 把parentGroup 交给他的父类， 他的父类用他管理全部的 将被创建Channel
        this.childGroup = ObjectUtil.checkNotNull(childGroup, "childGroup");

        //todo 方法链编程风格, 所以返回了当前的实例
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they get created
     * (after the acceptor accepted the {@link Channel}). Use a value of {@code null} to remove a previous set
     * {@link ChannelOption}.
     */
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        ObjectUtil.checkNotNull(childOption, "childOption");
        synchronized (childOptions) {
            if (value == null) {
                childOptions.remove(childOption);
            } else {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    /**
     * Set the specific {@link AttributeKey} with the given value on every child {@link Channel}. If the value is
     * {@code null} the {@link AttributeKey} is removed
     */
    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        ObjectUtil.checkNotNull(childKey, "childKey");
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    /**
     * Set the {@link ChannelHandler} which is used to serve the request for the {@link Channel}'s.
     */
    //todo 初始化添加的这个 ChannelHandler , 用服务于 所有的Channel 的请求
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        this.childHandler = ObjectUtil.checkNotNull(childHandler, "childHandler");
        return this;
    }

    //todo 这是ServerBootStrapt对 他父类初始化 channel的实现, 用于初始化 NioServerSocketChannel
    @Override
    void init(Channel channel) {
        //todo ChannelOption是在配置Channel的ChannelConfig的信息
        //todo 把NioserverSocketChannel和optionsMap传递进去，给Channel里面的属性赋值
        //todo 这些常量值全是关于和诸如TCP协议相关的信息
        // 设置 option
        setChannelOptions(channel, newOptionsArray(), logger);

        //todo 再次一波给Channel里面的属性赋值)是attrs0(获取到用户自定义的业务逻辑属性 --AttributeKey
        //todo 这个map中维护的是 程序运行时的 动态的 业务数据 , 可以实现让业务数据随着netty的运行原来存进去的数据还能取出来
        // 设置 attrs
        setAttributes(channel, newAttributesArray());


        //todo-------   options   attrs :都可以在创建BootStrap时动态的传递进去


        //todo ChannelPipeline 本身就是一个重要的组件，他里面是一个一个的处理器，说他是高级过滤器，交互的数据会一层一层经过它
        //todo 下面直接就调用了p，说明在channel调用pipeline方法之前，pipeline已经被创建出来了!
        //todo 到底是什么时候创建出来的?其实是在创建NioServerSocketChannel这个通道对象时，在他的顶级抽象父类(AbstractChannel)中创建了一个默认的pipeline对象
        //todo 补充: ChannelHandlerContext是ChannelHandler和Pipeline交互的桥梁
        ChannelPipeline p = channel.pipeline();

        //todo workerGroup处理IO线程
        final EventLoopGroup currentChildGroup = childGroup;

        //todo 我们自己添加的Initializer
        final ChannelHandler currentChildHandler = childHandler;

        //todo 这里是我们在Server类中添加的一些针对新连接channel的属性设置，这两者属性被acceptor使用到!!!
        final Entry<ChannelOption<?>, Object>[] currentChildOptions = newOptionsArray(childOptions);
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs = newAttributesArray(childAttrs);

        //ChannelInitializer一次性、初始化handler:
        //负责添加一个ServerBootstrapAcceptor handler，添加完后，自己就移除了:
        //ServerBootstrapAcceptor handler： 负责接收客户端连接创建连接后，对连接的初始化工作。

        //todo 下面的代码中是Netty原生默认会往NioServerSocketChannel的管道里面添加了一个ChannelInitializer，
        //todo 通过这个ChannelInitializer可以实现大批量的往pipeline中添加处理器
        //todo (后来我们自己添加的ChildHandler就继承了的这个ChannelInitializer，而这个就继承了的这个ChannelInitializer实现了ChannelHandler)
        p.addLast(new ChannelInitializer<Channel>() {

            //todo 这是个匿名内部类，一旦new就去执行它的构造方法群，完事后再回来看下面的代码，
            //todo 这个ChannelInitializer方便我们一次性往pipeline中添加多个处理器
            @Override
            public void initChannel(final Channel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                //todo 获取Bootstrap的handler对象，没有则返回空
                //todo 这个handler针对BossGroup的Channel，给他添加上我们在server类中添加的handler()里面添加处理器
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        //todo 这个很重要，在ServerBootStrap里面，netty已经为我们生成了接收器--!!!
                        //todo 专门处理新连接的接入，把新连接的channel绑定在workerGroup中的某一条线程上
                        //todo 用于处理用户的请求，但是还有没搞明白它是怎么触发执行的
                        pipeline.addLast(
                                //todo 这些参数是用户自定义的参数
                                //todo NioServerSocketChannel, worker线程组  处理器   关系的事件
                            new ServerBootstrapAcceptor(ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }

    @Override
    public ServerBootstrap validate() {
        super.validate();
        if (childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    //todo ServerBootstrapAcceptor 是内部类继承了ChannelInboundHandlerAdapter
    //todo ServerBootstrapAcceptor本质上它也是个handler，作用是把当前的channel传递给workergroup
    //todo 适配器模式，可以看到这个Acceptor是个入站处理器适配器，下面的重写了
    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        private final EventLoopGroup childGroup;
        private final ChannelHandler childHandler;
        private final Entry<ChannelOption<?>, Object>[] childOptions;
        private final Entry<AttributeKey<?>, Object>[] childAttrs;
        private final Runnable enableAutoReadTask;
        //接受连接后的后续处理
        ServerBootstrapAcceptor(
                final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler,
                Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {
            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;

            // 计划重新启用自动读取的任务。
            // Task which is scheduled to re-enable auto-read.

            // 在尝试提交之前创建这个 Runnable 非常重要，否则 URLClassLoader 可能无法加载类，因为它已经达到了文件限制。
            // It's important to create this Runnable before we try to submit it as otherwise the URLClassLoader may
            // not be able to load the class because of the file limit it already reached.
            //
            // See https://github.com/netty/netty/issues/1328
            enableAutoReadTask = new Runnable() {
                @Override
                public void run() {
                    channel.config().setAutoRead(true);
                }
            };
        }

        //todo 方法的是如何触发的?当新链接到来时，Selector会把连接交给服务端的NioMessageUnsafe进一步的IO操作，
        //todo read()后触发pipeline.fireChannelRead()事件从head传递到这里
        //todo 通过这个channelRead方法将当前连接的通道扔给了childGorup;
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;

            //todo 给这个来连接的通道添加childHandler，是我在Server中添加的childHandler，实际上是那个MyChannelInitializer，最终目的是添加handler
            child.pipeline().addLast(childHandler);

            //todo 给新来的Channel设置options选项
            setChannelOptions(child, childOptions, logger);

            //todo 给新来的Channel设置attr属性
            setAttributes(child, childAttrs);

            try {

                //todo 这里这!!把新的channel注册进childGroup
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }

        private static void forceClose(Channel child, Throwable t) {
            child.unsafe().closeForcibly();
            logger.warn("Failed to register an accepted channel: {}", child, t);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(enableAutoReadTask, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ServerBootstrap clone() {
        return new ServerBootstrap(this);
    }

    /**ls or {@code null}
     * Return the configured {@link EventLoopGroup} which will be used for the child channe
     * if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public EventLoopGroup childGroup() {
        return childGroup;
    }

    //todo 返回被子通道使用的ChannelHandler  , 如果为空返回 null
    final ChannelHandler childHandler() {
        return childHandler;
    }

    final Map<ChannelOption<?>, Object> childOptions() {
        synchronized (childOptions) {
            return copiedMap(childOptions);
        }
    }

    final Map<AttributeKey<?>, Object> childAttrs() {
        return copiedMap(childAttrs);
    }

    //todo 返回 ServerBootstrapConfig config
    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }
}
