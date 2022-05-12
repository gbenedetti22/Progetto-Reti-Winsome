package winsome.client.UI.banners;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentBanner extends PostBanner {
    private final String id;
    private final String date;

    private CommentBanner(String id, String author_name, String title, String content, String date, boolean deletable) {
        super(id, author_name, title, content, date, deletable, false);
        this.id = id;
        this.date = date;
    }

    public CommentBanner(String id, String author, String content, String date) {
        this(id, author, author, content, date, false);
    }

    @Override
    public int compareTo(PostBanner o) {
        if (!(o instanceof CommentBanner c)) return 0;
        if (id.equals(c.getID())) return 0;

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
        try {
            Date d1 = format.parse(this.date);
            Date d2 = format.parse(c.getDate());
            int compare = d1.compareTo(d2);

            return compare == 0 ? 1 : compare;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
