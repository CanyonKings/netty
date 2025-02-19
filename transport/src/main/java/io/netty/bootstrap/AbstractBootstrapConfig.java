/*
 * Copyright 2016 The Netty Project
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

import io.netty.channel.*;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Exposes the configuration of an {@link AbstractBootstrap}.
 */
public abstract class AbstractBootstrapConfig<B extends AbstractBootstrap<B, C>, C extends Channel> {

    protected final B bootstrap;

    protected AbstractBootstrapConfig(B bootstrap) {
        this.bootstrap = ObjectUtil.checkNotNull(bootstrap, "bootstrap");
    }

    /**
     * Returns the configured local address or {@code null} if non is configured yet.
     */
    public final SocketAddress localAddress() {
        return bootstrap.localAddress();
    }

    /**
     * Returns the configured {@link ChannelFactory} or {@code null} if non is configured yet.
     */
    //todo 返回channel工厂
    @SuppressWarnings("deprecation")
    public final ChannelFactory<? extends C> channelFactory() {
        return bootstrap.channelFactory();
    }

    /**
     * Returns the configured {@link ChannelHandler} or {@code null} if non is configured yet.
     */
    //todo ,返回bootStrap的handler的对象
    public final ChannelHandler handler() {
        return bootstrap.handler();
    }

    /**
     * Returns a copy of the configured options.
     */
    //todo  返回一份options的拷贝
    public final Map<ChannelOption<?>, Object> options() {
        return bootstrap.options();
    }

    /**
     * Returns a copy of the configured attributes.
     */
    //todo  返回一份 attributes 的拷贝
    public final Map<AttributeKey<?>, Object> attrs() {
        return bootstrap.attrs();
    }

    /**
     * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
     */
    //todo 返回配置好的  事件循环组
    @SuppressWarnings("deprecation")
    public final EventLoopGroup group() {
        return bootstrap.group();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
                .append(StringUtil.simpleClassName(this))
                .append('(');
        EventLoopGroup group = group();
        if (group != null) {
            buf.append("group: ")
                    .append(StringUtil.simpleClassName(group))
                    .append(", ");
        }
        @SuppressWarnings("deprecation")
        ChannelFactory<? extends C> factory = channelFactory();
        if (factory != null) {
            buf.append("channelFactory: ")
                    .append(factory)
                    .append(", ");
        }
        SocketAddress localAddress = localAddress();
        if (localAddress != null) {
            buf.append("localAddress: ")
                    .append(localAddress)
                    .append(", ");
        }

        Map<ChannelOption<?>, Object> options = options();
        if (!options.isEmpty()) {
            buf.append("options: ")
                    .append(options)
                    .append(", ");
        }
        Map<AttributeKey<?>, Object> attrs = attrs();
        if (!attrs.isEmpty()) {
            buf.append("attrs: ")
                    .append(attrs)
                    .append(", ");
        }
        ChannelHandler handler = handler();
        if (handler != null) {
            buf.append("handler: ")
                    .append(handler)
                    .append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }
}
