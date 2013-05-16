package edu.ucsc.ib.server;

import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Generic class for marshalling/unmarshalling XML files via JAXB.
 * 
 * @author chrisw
 * 
 */
public class JaxbConverter {

	private final boolean isLoggerOn = true;
	private final JAXBContext jaxbContext;

	/**
	 * Find the JAXB context in the defined path.
	 * 
	 * @param contextPath
	 *            Something like "edu.rpi.cs.xgmml".
	 * @throws JAXBException
	 */
	public JaxbConverter(String contextPath) throws JAXBException {
		this.jaxbContext = JAXBContext.newInstance(contextPath);
	}

	/**
	 * Print a message to System.out .
	 * 
	 * @param message
	 */
	private void log(String message) {
		if (isLoggerOn) {
			System.out.println("JaxbConverter: " + message);
		}
	}

	/**
	 * Unmarshal (deserialize) XML data. To get the object bound to the element,
	 * use JAXBElement.getValue() and then cast it to the correct class:
	 * <p>
	 * <code>Foo foo = (Foo) element.getValue(); </code>
	 * 
	 * @param reader
	 * @return
	 * @throws JAXBException
	 */
	public JAXBElement<?> unmarshal(Reader reader) throws JAXBException {
		Unmarshaller u = jaxbContext.createUnmarshaller();

		JAXBElement<?> element = (JAXBElement<?>) u.unmarshal(reader);

		return element;
	}

	/**
	 * Marshal (serialize) XML data. To create an element for marshalling use
	 * something like:
	 * <p>
	 * <code>JAXBElement&lt;Foo&gt; zooInfoElement = (new
	 * ObjectFactory()).createFoo(foo); </code>
	 * 
	 * @param jaxbElement
	 * @param writer
	 * @throws JAXBException
	 */
	public void marshal(JAXBElement<?> jaxbElement, Writer writer)
			throws JAXBException {
		Marshaller m = jaxbContext.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		m.marshal(jaxbElement, writer);
	}
}
