package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** timestamp of the Commit */
    private Date date;

    /** remember the file of commit {file name, material of the file in hash code}*/
    private Map<String,String> blobs;

    /** parent of the file */
    private String parent;
    private String parent1;
    private String parent2;
    /** how many commits */
    private int len;

    /** Constructor of Commit class */
    public Commit(String message, Date date, String parent, Map<String,String> blobs){
        this.message = message;
        this.date = date;
        this.parent = parent;
        this.blobs = blobs;
        this.len = 0;
    }

    public void parent1(String parent1){
        this.parent1 = parent1;
    }

    public void parent2(String parent2){
        this.parent2 = parent2;
    }

    /** return the length of the commit */
    public int length(){
        return len;
    }

    /** increment length */
    public void length_increment(int len){
        this.len = this.len + len;
    }

    /** returns the SHA-1 hash of commit */
    public String sha(){
        return Utils.sha1(Utils.serialize(this));
    }

    /** return the parent */
    public String parent(){
        return parent;
    }

    /** return the blobs of the commit */
    public Map<String,String > blobs(){
        return blobs;
    }

    /** return the time of commit */
    public Date date(){
        return date;
    }

    /** return the message of the commit */
    public String message(){
        return message;
    }

    /** return whether two commits are equals */
    public boolean equals(Commit c){
        return c.sha().equals(this.sha());
    }

    /** return the parent of the commit */

    /* TODO: fill in the rest of this class. */
}
