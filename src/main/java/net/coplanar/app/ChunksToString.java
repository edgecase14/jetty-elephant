/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.CompletableTask;
import org.eclipse.jetty.util.Utf8StringBuilder;

/**
 *
 * @author jetty docs
 */

// CompletableTask is-a CompletableFuture.
public class ChunksToString extends CompletableTask<String>
{
    private final List<Content.Chunk> chunks = new ArrayList<>();
    private final Content.Source source;

    public ChunksToString(Content.Source source)
    {
        this.source = source;
    }

    @Override
    public void run()
    {
        while (true)
        {
            // Read a chunk, must be eventually released.
            Content.Chunk chunk = source.read(); 

            if (chunk == null)
            {
                source.demand(this);
                return;
            }

            if (Content.Chunk.isFailure(chunk))
            {
                //handleFatalFailure(chunk.getFailure());
                //Throwable cf = chunk.getFailure();
                System.out.println("chunk failure");
                return;
            }

            // A normal chunk of content, consume it.
            consume(chunk);

            // Release the chunk.
            // This pairs the call to read() above.
            chunk.release(); 

            if (chunk.isLast())
            {
                // Produce the result.
                String result = getResult();

                // Complete this CompletableFuture with the result.
                complete(result);

                // The reading is complete.
                return;
            }
        }
    }

    public void consume(Content.Chunk chunk)
    {
        // The chunk is not consumed within this method, but
        // stored away for later use, so it must be retained.
        chunk.retain(); 
        chunks.add(chunk);
    }

    public String getResult()
    {
        Utf8StringBuilder builder = new Utf8StringBuilder();
        // Iterate over the chunks, copying and releasing.
        for (Content.Chunk chunk : chunks)
        {
            // Copy the chunk bytes into the builder.
            builder.append(chunk.getByteBuffer());

            // The chunk has been consumed, release it.
            // This pairs the retain() in consume().
            chunk.release(); 
        }
        return builder.toCompleteString();
    }
}