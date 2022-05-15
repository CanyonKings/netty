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
 * ��д������Future
 */
public interface Promise<V> extends Future<V> {

    /**
     * Marks this future as a success and notifies all listeners.
     * �����future���Ϊһ���ɹ�����֪ͨ���еļ�����
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * ������Ѿ��ɹ���ʧ���ˣ������׳�һ��Υ��״̬�쳣
     */
    Promise<V> setSuccess(V result);

    /**
     * Marks this future as a success and notifies all listeners.
     * �����future���Ϊһ���ɹ�����֪ͨ���еļ�����
     * @return {@code true} if and only if successfully marked this future as a success.
     *         Otherwise {@code false} because this future is already marked as either a success or a failure.
     * ����ture ���ҽ����ɹ��ر�־�����future�ǳɹ���
     * ���򷵻�false����Ϊ���δ���Ѿ������Ϊ�ɹ���ʧ��
     */
    boolean trySuccess(V result);

    /**
     * Marks this future as a failure and notifies all listeners.
     * �����future���Ϊһ��ʧ�ܣ���֪ͨ���еļ�����
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * ������Ѿ��ɹ���ʧ���ˣ������׳�һ��Υ��״̬�쳣
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * Marks this future as a failure and notifies all listeners.
     * �����future���Ϊһ��ʧ�ܣ���֪ͨ���еļ�����
     *
     * @return {@code true} if and only if successfully marked this future as a failure.
     * Otherwise {@code false} because this future is already marked as either a success or a failure.
     * ����ture ���ҽ����ɹ��ر�־�����future��ʧ�ܵ�
     * ���򷵻�false����Ϊ���δ���Ѿ������Ϊ�ɹ���ʧ��
     */
    boolean tryFailure(Throwable cause);

    /**
     * Make this future impossible to cancel.
     * �����future�޷�ȡ��
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done without being cancelled.
     *         {@code false} if this future has been cancelled already.
     * ���ҽ����ɹ��ر�����future �ǲ��ɳ����� �����Ѿ���ɶ���δ��������ture
     * ������ future �Ѿ�����������false
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
