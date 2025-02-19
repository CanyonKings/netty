/*
 * Copyright 2013 The Netty Project
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
package io.netty.util.concurrent;

/**
 * Special {@link Future} which is writable.
 * 可写的特殊Future
 */
public interface Promise<V> extends Future<V> {

    /**
     * Marks this future as a success and notifies all listeners.
     * 将这个future标记为一个成功，并通知所有的监听器
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * 如果它已经成功或失败了，它将抛出一个违法状态异常
     */
    Promise<V> setSuccess(V result);

    /**
     * Marks this future as a success and notifies all listeners.
     * 将这个future标记为一个成功，并通知所有的监听器
     * @return {@code true} if and only if successfully marked this future as a success.
     *         Otherwise {@code false} because this future is already marked as either a success or a failure.
     * 返回ture 当且仅当成功地标志着这个future是成功的
     * 否则返回false，因为这个未来已经被标记为成功或失败
     */
    boolean trySuccess(V result);

    /**
     * Marks this future as a failure and notifies all listeners.
     * 将这个future标记为一个失败，并通知所有的监听器
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * 如果它已经成功或失败了，它将抛出一个违法状态异常
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * Marks this future as a failure and notifies all listeners.
     * 将这个future标记为一个失败，并通知所有的监听器
     *
     * @return {@code true} if and only if successfully marked this future as a failure.
     * Otherwise {@code false} because this future is already marked as either a success or a failure.
     * 返回ture 当且仅当成功地标志着这个future是失败的
     * 否则返回false，因为这个未来已经被标记为成功或失败
     */
    boolean tryFailure(Throwable cause);

    /**
     * Make this future impossible to cancel.
     * 让这个future无法取消
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done without being cancelled.
     *         {@code false} if this future has been cancelled already.
     * 当且仅当成功地标记这个future 是不可撤销的 或者已经完成而从未撤销返回ture
     * 如果这个 future 已经被撤销返回false
     *
     */
    boolean setUncancellable();

    @Override
    Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
