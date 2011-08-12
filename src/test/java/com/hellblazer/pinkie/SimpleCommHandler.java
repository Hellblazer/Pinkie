/** (C) Copyright 2011 Hal Hildebrand, all rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.pinkie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class SimpleCommHandler implements CommunicationsHandler {

    final List<byte[]>                          reads     = new ArrayList<byte[]>();
    final List<ByteBuffer>                      writes    = new CopyOnWriteArrayList<ByteBuffer>();
    final AtomicBoolean                         accepted  = new AtomicBoolean();
    final AtomicBoolean                         connected = new AtomicBoolean();
    final AtomicReference<SocketChannelHandler> handler   = new AtomicReference<SocketChannelHandler>();

    @Override
    public void handleAccept(SocketChannel channel, SocketChannelHandler handler) {
        this.handler.set(handler);
        accepted.set(true);
    }

    @Override
    public void handleConnect(SocketChannel channel,
                              SocketChannelHandler handler) {
        connected.set(true);
    }

    @Override
    public void handleRead(SocketChannel channel) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);
            for (int read = channel.read(buffer); read != 0; read = channel.read(buffer)) {
                buffer.flip();
                byte[] b = new byte[read];
                buffer.get(b, 0, read);
                baos.write(b);
                buffer.flip();
            }
            reads.add(baos.toByteArray());
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        handler.get().selectForRead();
    }

    @Override
    public void handleWrite(SocketChannel channel) {
        if (writes.size() == 0) {
            return;
        }
        try {
            ByteBuffer buffer = writes.get(0);
            channel.write(buffer);
            if (!buffer.hasRemaining()) {
                writes.remove(0);
            } else {
                handler.get().selectForWrite();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void selectForRead() {
        handler.get().selectForRead();
    }

    public void selectForWrite() {
        handler.get().selectForWrite();
    }

    @Override
    public void closing(SocketChannel channel) {
    }
}
