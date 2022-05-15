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
 * �첽�����Ľ����
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed successfully.
     * ���ҽ��� i/o �����ɹ����ʱ������true
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     * ���ҽ�������ͨ��cancel����ʱ������true
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has failed.
     * �� i/o ����ʧ�ܣ��򷵻� i/o ����ʧ�ܵ�ԭ��
     *
     * @return the cause of the failure.
     * ����ʧ�ܵ�ԭ��
     * {@code null} if succeeded or this future is not completed yet.
     * ����ɹ����������δ����δ��ɷ���null��
     */
    Throwable cause();

    /**
     * ����������
     * Adds the specified listener to this future.
     * ��ָ���ļ�������ӵ���future��
     * The specified listener is notified when this future is {@linkplain #isDone() done}.
     * �����future�����ʱ��������ָ֪ͨ������������
     * If this future is already completed, the specified listener is notified immediately.
     * ������future�Ѿ���ɣ�������ָ֪ͨ������������
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * ���������
     * Adds the specified listeners to this future.
     * The specified listeners are notified when this future is {@linkplain #isDone() done}.
     * If this future is already completed, the specified listeners are notified immediately.
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * ����������
     * Removes the first occurrence of the specified listener from this future.
     * �Ӵ�future�Ƴ�ָ���������ĵ�һ��ƥ���
     * The specified listener is no longer notified when this future is {@linkplain #isDone() done}.
     * �����future�����ʱ������ָ֪ͨ������������
     * If the specified listener is not associated with this future, this method does nothing and returns silently.
     * ���ָ�������������futureû�й�������˷�����ִ���κβ���������Ĭ���ء�
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * ���������
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this future is {@linkplain #isDone() done}.
     * If the specified listeners are not associated with this future, this method does nothing and returns silently.
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future failed.
     * ͬ���ȴ����future��ֱ������ɣ��������futureʧ��ʱ�׳�ʧ�ܵ�ԭ��
     */
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future failed.
     */
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     * �ȴ����future����ɡ�
     * @throws InterruptedException if the current thread was interrupted
     * �����ǰ�̱߳��жϾ��׳��ж��쳣
     */
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without interruption.
     * �ȴ����future���ܸ��ŵ���ɡ�
     * This method catches an {@link InterruptedException} and discards it silently.
     * �����������һ���ж��쳣��ĬĬ�ض�������
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the specified time limit.
     * �ȴ����future��ָ����ʱ����������ɡ�
     * @return {@code true} if and only if the future was completed within the specified time limit
     * ���ҽ���δ����ָ����ʱ������ɷ���ture
     * @throws InterruptedException if the current thread was interrupted
     * �����ǰ�̱߳��жϾ��׳��ж��쳣
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the specified time limit.
     *
     * @return {@code true} if and only if the future was completed within the specified time limit
     *
     * @throws InterruptedException if the current thread was interrupted
     * �����ǰ�̱߳��жϾ��׳��ж��쳣
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the specified time limit without interruption.
     * �ȴ���future��ָ����ʱ����������ɣ����жϡ�
     * This method catches an {@link InterruptedException} and discards it silently.
     * ���ķ�������һ���ж��쳣 ��Ȼ��ĬĬ�ض�������
     * @return {@code true} if and only if the future was completed within the specified time limit
     * ���ҽ���future��ָ����ʱ������ɷ��� true
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
     * �������ط��ؽ����
     *
     * If the future is not done yet this will return {@code null}.
     * ���δ���δ��������������null��
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     * ��Ϊ�п���ʹ��nullֵ�����future�ǳɹ��ģ�����������Ҫ���future�Ƿ������isdone��ɣ��������������ص�nullֵ��
     */
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with a {@link CancellationException}.
     * ���ȡ���ǳɹ��ģ�����ʧ�ܵ�future��{@link CancellationException }��
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
