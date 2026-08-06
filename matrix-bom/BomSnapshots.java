import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Prints matrix Version properties whose values are SNAPSHOT versions. */
public class BomSnapshots {
  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) {
      throw new IllegalArgumentException("usage: java BomSnapshots.java bom.xml [--all]");
    }
    boolean all = args.length == 2 && "--all".equals(args[1]);
    if (args.length == 2 && !all) {
      throw new IllegalArgumentException("unknown option: " + args[1]);
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    Document document = factory.newDocumentBuilder().parse(new java.io.File(args[0]));
    NodeList properties = document.getDocumentElement().getElementsByTagNameNS("*", "properties");
    for (int i = 0; i < properties.getLength(); i++) {
      Node propertiesNode = properties.item(i);
      if (propertiesNode.getParentNode() != document.getDocumentElement()) {
        continue;
      }
      for (Node node = propertiesNode.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        String name = node.getLocalName();
        String value = node.getTextContent().trim();
        if (name.startsWith("matrix") && name.endsWith("Version") && (all || value.endsWith("-SNAPSHOT"))) {
          System.out.println(name + "=" + value);
        }
      }
    }
  }
}
