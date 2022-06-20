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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous operation.
 * 异步操作的结果。
 */
//todo 这个接口继承了 java并发包总的Futrue，并在其基础上增加了很多方法
//todo Future表示对未来任务的封装
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed successfully.
     * 当且仅当 i/o 操作成功完成时，返回true
     */
    //todo 判断IO是否成功返回
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     * 当且仅当可以通过cancel撤销时，返回true
     */
    //todo 判断是否是 cancel()方法取消
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has failed.
     * 当 i/o 操作失败，则返回 i/o 操作失败的原因。
     *
     * @return the cause of the failure.
     * 返回失败的原因
     * {@code null} if succeeded or this future is not completed yet.
     * 如果成功，或者这个未来尚未完成返回null。
     */
    //todo 返回IO 操作失败的原因
    Throwable cause();

    /**
     * 单个监听器
     * Adds the specified listener to this future.
     * 将指定的监听器添加到此future。
     * The specified listener is notified when this future is {@linkplain #isDone() done}.
     * 当这个future是完成时，会立即通知指定的侦听器。
     * If this future is already completed, the specified listener is notified immediately.
     * 如果这个future已经完成，则立即通知指定的侦听器。
     */
    //todo 使用了观察者设计模式, 给这个future添加监听器, 一旦Future 完成, listenner 立即被通知
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * 多个监听器
     * Adds the specified listeners to this future.
     * The specified listeners are notified when this future is {@linkplain #isDone() done}.
     * If this future is already completed, the specified listeners are notified immediately.
     */
    //todo 添加多个listenner
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * 单个监听器
     * Removes the first occurrence of the specified listener from this future.
     * 从此future移除指定侦听器的第一个匹配项。
     * The specified listener is no longer notified when this future is {@linkplain #isDone() done}.
     * 当这个future是完成时，不再通知指定的侦听器。
     * If the specified listener is not associated with this future, this method does nothing and returns silently.
     * 如果指定的侦听器与此future没有关联，则此方法不执行任何操作，并静默返回。
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * 多个监听器
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this future is {@linkplain #isDone() done}.
     * If the specified listeners are not associated with this future, this method does nothing and returns silently.
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future failed.
     * 同步等待这个future，直到它完成，并在这个future失败时抛出失败的原因。
     */
    //todo sync(同步) 等待着 future 的完成, 并且,一旦future失败了,就会抛出 future 失败的原因
    //todo bind()是个异步操作,我们需要同步等待他执行成功
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future failed.
     */
    //todo 不会被中断的 sync等待
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     * 等待这个future的完成。
     * @throws InterruptedException if the current thread was interrupted
     * 如果当前线程被中断就抛出中断异常
     */
    //todo 等待
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without interruption.
     * 等待这个future不受干扰地完成。
     * This method catches an {@link InterruptedException} and discards it silently.
     * 这个方法捕获一个中断异常并默默地丢弃它。
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the specified time limit.
     * 等待这个future在指定的时间限制内完成。
     * @return {@code true} if and only if the future was completed within the specified time limit
     * 当且仅当未来在指定的时限内完成返回ture
     * @throws InterruptedException if the current thread was interrupted
     * 如果当前线程被中断就抛出中断异常
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the specified time limit.
     *
     * @return {@code true} if and only if the future was completed within the specified time limit
     *
     * @throws InterruptedException if the current thread was interrupted
     * 如果当前线程被中断就抛出中断异常
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the specified time limit without interruption.
     * 等待此future在指定的时间限制内完成，不中断。
     * This method catches an {@link InterruptedException} and discards it silently.
     * 他的方法捕获一个中断异常 ，然后默默地丢弃它。
     * @return {@code true} if and only if the future was completed within the specified time limit
     * 当且仅当future在指定的时限内完成返回 true
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the specified time limit without interruption.
     * This method catches an {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking.
     * 不阻塞地返回结果。
     *
     * If the future is not done yet this will return {@code null}.
     * 如果未完成未来操作，将返回null。
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     * 因为有可能使用null值来标记future是成功的，所以您还需要检查future是否真的用isdone完成，而不是依赖返回的null值。
     */
    //todo 无阻塞的返回Future对象, 如果没有,返回null
    //todo 有时future成功执行后返回值为null，这是null就是成功的标识，如 Runable就没有返回值，因此文档建议还要 通过isDone() 判断一下真的完成了吗
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with a {@link CancellationException}.
     * 如果取消是成功的，它将失败的future与{@link CancellationException }。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
