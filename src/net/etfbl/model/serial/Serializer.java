package net.etfbl.model.serial;

import java.io.IOException;

public abstract class Serializer<T> {
	public abstract void serialize(T data, String filePath) throws IOException;
	public abstract T deserialize(String filePath) throws IOException, ClassCastException;
}
