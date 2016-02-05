package ml.hlaaftana.discordg.util

import java.nio.*
import net.tomp2p.opuswrapper.Opus

import com.sun.jna.Native
import com.sun.jna.ptr.PointerByReference

// copied from https://github.com/tbocek/opus-wrapper/blob/master/src/test/java/net/tomp2p/opuswrapper/OpusExample.java
class AudioUtil {
	static int sampleRate = 48000
	static int channels = 2
	static int frameSize = 960
	static int frameTimeAmount = 20

	static {
		try{
			System.loadLibrary("opus")
		}catch (UnsatisfiedLinkError ex1){
			try {
				File f = Native.extractFromResourcePath("opus")
				System.load(f.absolutePath)
			}catch (Exception ex2) {
				ex1.printStackTrace()
				ex2.printStackTrace()
			}
		}
	}

	static ShortBuffer decode(List<ByteBuffer> packets) {
		IntBuffer error = IntBuffer.allocate(4)
		PointerByReference opusDecoder = Opus.INSTANCE.opus_decoder_create(sampleRate, channels, error)

		ShortBuffer shortBuffer = ShortBuffer.allocate(1024 * 1024)
		for (dataBuffer in packets) {
			byte[] transferedBytes = new byte[dataBuffer.remaining()]
			dataBuffer.get(transferedBytes)
			int decoded = Opus.INSTANCE.opus_decode(opusDecoder, transferedBytes, transferedBytes.length,
					shortBuffer, frameSize, 0)
			shortBuffer.position(shortBuffer.position() + decoded)
		}
		shortBuffer.flip()

		Opus.INSTANCE.opus_decoder_destroy(opusDecoder)
		return shortBuffer
	}

	static List<ByteBuffer> encode(ShortBuffer shortBuffer) {
		IntBuffer error = IntBuffer.allocate(4)
		PointerByReference opusEncoder = Opus.INSTANCE.opus_encoder_create(sampleRate, channels,
				Opus.OPUS_APPLICATION_RESTRICTED_LOWDELAY, error)
		int read = 0
		List<ByteBuffer> list = new ArrayList<>()
		while (shortBuffer.hasRemaining()) {
			ByteBuffer dataBuffer = ByteBuffer.allocate(1024)
			int toRead = Math.min(shortBuffer.remaining(), dataBuffer.remaining())
			read = Opus.INSTANCE.opus_encode(opusEncoder, shortBuffer, frameSize, dataBuffer, toRead)
			dataBuffer.position(dataBuffer.position() + read)
			dataBuffer.flip()
			list.add(dataBuffer)
			shortBuffer.position(shortBuffer.position() + frameSize)
		}
		Opus.INSTANCE.opus_encoder_destroy(opusEncoder)
		shortBuffer.flip()
		return list
	}
}
