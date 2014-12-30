package cat.udl.eps.softarch.hello.util;


import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.xquery.*;


public class XQueryHelper {
    private static final Logger log = Logger.getLogger(XQueryHelper.class.getName());

    private XQPreparedExpression expr;
    private XQConnection conn;

    private JAXBContext jaxbContext;
    private Unmarshaller jaxbUnmarshaller;
    static final String meteocatURL = "http://static-m.meteo.cat/content/opendata/ctermini_comarcal.xml";
    static final String apiURL = "http://musicbrainz.org/ws/2/artist/cc2c9c3c-b7bc-4b8b-84d8-4fbd8779e493?inc=releases";
    static final String albumsXQ =
            "declare namespace mmd=\"http://musicbrainz.org/ns/mmd-2.0#\";\n"
                    + "declare variable $doc external;\n"
                    + "for $r in $doc//mmd:release\n"
                    + "let $years-from-date:=$r/mmd:date[matches(text(),\"^\\d{4}-\\d{2}-\\d{2}$\")]/year-from-date(text())\n"
                    + "let $years:=$r/mmd:date[matches(text(),\"^\\d{4}$\")]\n"
                    + "return\n"
                    + "<song>\n"
                    + "  <title>{$r/mmd:title/text()}</title>\n"
                    + "  <artist>{$doc//mmd:artist/mmd:name/text()}</artist>\n"
                    + "  <countries>{distinct-values($r//mmd:country)}</countries>\n"
                    + "  <year>{min(($years,$years-from-date))}</year>\n"
                    + "</song>";

    @XmlRootElement
    private static class Song {
        @XmlElement
        String title;
        @XmlElement
        String artist;
        @XmlElement
        String countries;
        @XmlElement
        Integer year;

        @Override
        public String toString() {
            return "Title: " + title + "\n" + "Artist: " + artist + "\n" + "Countries: " + countries + "\n" + "Year: " + year + "\n";
        }
    }

    XQueryHelper(String xquery, URL url)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, XQException, IOException, JAXBException {
        URLConnection urlconn = url.openConnection();
        urlconn.setReadTimeout(50000);

        XQDataSource xqds = (XQDataSource) Class.forName("net.sf.saxon.xqj.SaxonXQDataSource").newInstance();
        this.conn = xqds.getConnection();
        this.expr = conn.prepareExpression(xquery);
        this.expr.bindDocument(new javax.xml.namespace.QName("doc"), urlconn.getInputStream(), null, null);

        this.jaxbContext = JAXBContext.newInstance(Song.class);
        this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    }

    ArrayList<Song> getSongs() {
        ArrayList<Song> songs = new ArrayList<Song>();
        try {
            XQResultSequence rs = this.expr.executeQuery();
            while (rs.next()) {
                XQItem item = rs.getItem();
                Song song = (Song) jaxbUnmarshaller.unmarshal(item.getNode());
                songs.add(song);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
        } finally {
            close();
        }
        return songs;
    }

    public String getRegionWeather(int region) throws XQException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {

        String xquery = "declare variable $doc := doc(\"http://static-m.meteo.cat/content/opendata/ctermini_comarcal.xml\");\n"
                + "for $c in $doc//prediccio[@idcomarca=\"1\"]/variable[@dia=\"1\"]\n"
                + "return data($c/@simbolmati)";

        URLConnection urlconn = null;
        urlconn = new URL(meteocatURL).openConnection();
        XQDataSource xqds = (XQDataSource) Class.forName("net.sf.saxon.xqj.SaxonXQDataSource").newInstance();
        this.conn = xqds.getConnection();
        this.expr = conn.prepareExpression(xquery);
        urlconn.setReadTimeout(50000);
        XQResultSequence rs = this.expr.executeQuery();
        return rs.getAtomicValue();

    }

    private void close() {
        try {
            this.expr.close();
            this.conn.close();
        } catch (XQException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    public static void prova() {

        try {
            XQueryHelper xQueryHelper = new XQueryHelper(albumsXQ, new URL(apiURL));
            ArrayList<Song> songs = xQueryHelper.getSongs();
            for (Song song : songs)
                System.out.println(song);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        try {
            XQueryHelper xQueryHelper = new XQueryHelper(albumsXQ, new URL(apiURL));
            ArrayList<Song> songs = xQueryHelper.getSongs();
            for (Song song : songs)
                System.out.println(song);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}