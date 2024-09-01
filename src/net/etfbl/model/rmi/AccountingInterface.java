package net.etfbl.model.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import net.etfbl.model.Book;
import net.etfbl.model.Receipt;

public interface AccountingInterface extends Remote {
	public Receipt formReceipt(List<Book> books) throws RemoteException;
}
