package net.etfbl.model.serial;

import java.beans.*;
import java.io.*;

public class XMLSerializer<T> extends Serializer<T> {
	@Override
	public void serialize(T data, String filePath) throws IOException {
		System.out.println(data);
        OutputStream fos = new FileOutputStream(filePath);
        XMLEncoder encoder = new XMLEncoder(fos);

        encoder.writeObject(data);

        encoder.close();
        fos.close();
	}
	
	@Override
	public T deserialize(String filePath) throws IOException, ClassCastException {
		XMLDecoder decoder = new XMLDecoder(new FileInputStream(filePath));
		
		@SuppressWarnings("unchecked")
		T result = (T)decoder.readObject();
		
		decoder.close();
		return result;
	}
}
