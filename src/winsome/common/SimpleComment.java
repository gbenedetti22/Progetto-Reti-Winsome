package winsome.common;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
/*
    Versione ridotta della classe Comment del DB
 */
public class SimpleComment implements Comparable<SimpleComment>, Serializable {
    private final String id;
    private final String author;
    private final String content;
    private final String date;

    public SimpleComment(String id, String author, String content, String date) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    @Override
    public int compareTo(SimpleComment o) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
        try {
            Date d1 = sdf.parse(date);
            Date d2 = sdf.parse(o.getDate());

            return d1.compareTo(d2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
