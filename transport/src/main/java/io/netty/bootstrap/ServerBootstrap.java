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
//todo ServerBootstrap ��һ��������,�������Ǻ����ɵ�����һ���������˵�ͨ��
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);

    // The order in which child ChannelOptions are applied is important they may depend on each other for validation
    // purposes.
    //todo �����ǹ���Э�����������Ϣ�ķ�װ
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    //todo ����������ʱ�û��洢��ȥ�����ݵķ�װ����
    private final Map<AttributeKey<?>, Object> childAttrs = new ConcurrentHashMap<AttributeKey<?>, Object>();

    // ����
    //todo ����ServerBootstraptConfig
    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);

    //todo volatileȷ������ʱ�������ָ���������
    //todo ��ȷ happen �� before �Ĺ�ϵ
    private volatile EventLoopGroup childGroup;

    //todo ������ childGroup, �������ѭ����������
    private volatile ChannelHandler childHandler;

    public ServerBootstrap() { }

    /**
     * ��¡bootstrap ʹ�ñ����췽��
     * @param bootstrap
     */
    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        //todo  ��NioServerBootstrapt�Ƕ�����,  ʵ���ϲ�û��ִ��������캯��,ȥ��ʼ�������childGroup  -- ����IO�¼����¼�ѭ����
        //todo ����������Ĵ�Լ100�еĵط���ʼ����, ����ȥ��100��
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
    //todo ��ʼ����������� parentGroup �ʹ�������� childGroup,  �������¼�ѭ����,������ �������е� ServerChannel �� Channel
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        //todo ���ø���AbstractBootstrap �Ĺ���, ���ݽ�ȥ parentGroup �� ��parentGroup �������ĸ��࣬ ���ĸ�����������ȫ���� ��������Channel
        super.group(parentGroup);
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        //todo ���ø���AbstractBootstrap �Ĺ���, ���ݽ�ȥ parentGroup �� ��parentGroup �������ĸ��࣬ ���ĸ�����������ȫ���� ��������Channel
        this.childGroup = ObjectUtil.checkNotNull(childGroup, "childGroup");

        //todo ��������̷��, ���Է����˵�ǰ��ʵ��
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
    //todo ��ʼ����ӵ���� ChannelHandler , �÷����� ���е�Channel ������
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        this.childHandler = ObjectUtil.checkNotNull(childHandler, "childHandler");
        return this;
    }

    //todo ����ServerBootStrapt�� �������ʼ�� channel��ʵ��, ���ڳ�ʼ�� NioServerSocketChannel
    @Override
    void init(Channel channel) {
        //todo ChannelOption��������Channel��ChannelConfig����Ϣ
        //todo ��NioserverSocketChannel��optionsMap���ݽ�ȥ����Channel��������Ը�ֵ
        //todo ��Щ����ֵȫ�ǹ��ں�����TCPЭ����ص���Ϣ
        // ���� option
        setChannelOptions(channel, newOptionsArray(), logger);

        //todo �ٴ�һ����Channel��������Ը�ֵ)��attrs0(��ȡ���û��Զ����ҵ���߼����� --AttributeKey
        //todo ���map��ά������ ��������ʱ�� ��̬�� ҵ������ , ����ʵ����ҵ����������netty������ԭ�����ȥ�����ݻ���ȡ����
        // ���� attrs
        setAttributes(channel, newAttributesArray());


        //todo-------   options   attrs :�������ڴ���BootStrapʱ��̬�Ĵ��ݽ�ȥ


        //todo ChannelPipeline �������һ����Ҫ���������������һ��һ���Ĵ�������˵���Ǹ߼������������������ݻ�һ��һ�㾭����
        //todo ����ֱ�Ӿ͵�����p��˵����channel����pipeline����֮ǰ��pipeline�Ѿ�������������!
        //todo ������ʲôʱ�򴴽�������?��ʵ���ڴ���NioServerSocketChannel���ͨ������ʱ�������Ķ���������(AbstractChannel)�д�����һ��Ĭ�ϵ�pipeline����
        //todo ����: ChannelHandlerContext��ChannelHandler��Pipeline����������
        ChannelPipeline p = channel.pipeline();

        //todo workerGroup����IO�߳�
        final EventLoopGroup currentChildGroup = childGroup;

        //todo �����Լ���ӵ�Initializer
        final ChannelHandler currentChildHandler = childHandler;

        //todo ������������Server������ӵ�һЩ���������channel���������ã����������Ա�acceptorʹ�õ�!!!
        final Entry<ChannelOption<?>, Object>[] currentChildOptions = newOptionsArray(childOptions);
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs = newAttributesArray(childAttrs);

        //ChannelInitializerһ���ԡ���ʼ��handler:
        //�������һ��ServerBootstrapAcceptor handler���������Լ����Ƴ���:
        //ServerBootstrapAcceptor handler�� ������տͻ������Ӵ������Ӻ󣬶����ӵĳ�ʼ��������

        //todo ����Ĵ�������Nettyԭ��Ĭ�ϻ���NioServerSocketChannel�Ĺܵ����������һ��ChannelInitializer��
        //todo ͨ�����ChannelInitializer����ʵ�ִ���������pipeline����Ӵ�����
        //todo (���������Լ���ӵ�ChildHandler�ͼ̳��˵����ChannelInitializer��������ͼ̳��˵����ChannelInitializerʵ����ChannelHandler)
        p.addLast(new ChannelInitializer<Channel>() {

            //todo ���Ǹ������ڲ��࣬һ��new��ȥִ�����Ĺ��췽��Ⱥ�����º��ٻ���������Ĵ��룬
            //todo ���ChannelInitializer��������һ������pipeline����Ӷ��������
            @Override
            public void initChannel(final Channel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                //todo ��ȡBootstrap��handler����û���򷵻ؿ�
                //todo ���handler���BossGroup��Channel�����������������server������ӵ�handler()������Ӵ�����
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        //todo �������Ҫ����ServerBootStrap���棬netty�Ѿ�Ϊ���������˽�����--!!!
                        //todo ר�Ŵ��������ӵĽ��룬�������ӵ�channel����workerGroup�е�ĳһ���߳���
                        //todo ���ڴ����û������󣬵��ǻ���û������������ô����ִ�е�
                        pipeline.addLast(
                                //todo ��Щ�������û��Զ���Ĳ���
                                //todo NioServerSocketChannel, worker�߳���  ������   ��ϵ���¼�
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

    //todo ServerBootstrapAcceptor ���ڲ���̳���ChannelInboundHandlerAdapter
    //todo ServerBootstrapAcceptor��������Ҳ�Ǹ�handler�������ǰѵ�ǰ��channel���ݸ�workergroup
    //todo ������ģʽ�����Կ������Acceptor�Ǹ���վ���������������������д��
    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        private final EventLoopGroup childGroup;
        private final ChannelHandler childHandler;
        private final Entry<ChannelOption<?>, Object>[] childOptions;
        private final Entry<AttributeKey<?>, Object>[] childAttrs;
        private final Runnable enableAutoReadTask;
        //�������Ӻ�ĺ�������
        ServerBootstrapAcceptor(
                final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler,
                Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {
            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;

            // �ƻ����������Զ���ȡ������
            // Task which is scheduled to re-enable auto-read.

            // �ڳ����ύ֮ǰ������� Runnable �ǳ���Ҫ������ URLClassLoader �����޷������࣬��Ϊ���Ѿ��ﵽ���ļ����ơ�
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

        //todo ����������δ�����?�������ӵ���ʱ��Selector������ӽ�������˵�NioMessageUnsafe��һ����IO������
        //todo read()�󴥷�pipeline.fireChannelRead()�¼���head���ݵ�����
        //todo ͨ�����channelRead��������ǰ���ӵ�ͨ���Ӹ���childGorup;
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;

            //todo ����������ӵ�ͨ�����childHandler��������Server����ӵ�childHandler��ʵ�������Ǹ�MyChannelInitializer������Ŀ�������handler
            child.pipeline().addLast(childHandler);

            //todo ��������Channel����optionsѡ��
            setChannelOptions(child, childOptions, logger);

            //todo ��������Channel����attr����
            setAttributes(child, childAttrs);

            try {

                //todo ������!!���µ�channelע���childGroup
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

    //todo ���ر���ͨ��ʹ�õ�ChannelHandler  , ���Ϊ�շ��� null
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

    //todo ���� ServerBootstrapConfig config
    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }
}
