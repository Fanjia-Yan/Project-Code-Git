package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Stage implements Serializable {

    /** a hashmap of the file that is on the staging area  {name of the file, content} */
    private HashMap<String,String> staging_area;

    /** A LinkedList of Removal File */
    private LinkedList<String> file_for_removal;

    /** the head of the commit */
    private Commit head;

    /** constructor */
    public Stage(){
        staging_area = new HashMap<>();
        file_for_removal = new LinkedList<>();
    }

    /** set the head of the commit */
    public void set_head(Commit head){
        this.head = head;
    }

    /** add the file to the LinkedList */
    public void add(String file){
        File add_file = Utils.join(Repository.CWD,file);
        if (file_for_removal.contains(file)){
            String retrive_info = head.blobs().get(file);
            File blobfile = Utils.join(Repository.BLOBS_DIR,retrive_info);
            byte[] retrive_content = Utils.readContents(blobfile);
            Utils.writeContents(add_file,retrive_content);
            file_for_removal.remove(file);
            return;
        }
        if (!add_file.exists()){
            System.out.println("File does not exits.");
            return;
        }
        String add_file_object = Utils.readContentsAsString(add_file);
        String add_file_sha = Utils.sha1(add_file_object);

        /** from the blob to get the filename's file hashcode and see if they are equal */
        String if_exist_file = head.blobs().get(file);
        if (if_exist_file == null) {
            staging_area.put(file, add_file_object);
        } else {
            if (add_file_sha.equals(if_exist_file)) {
                staging_area.remove(file);
            } else {
                staging_area.put(file, add_file_object);
            }
        }
        file_for_removal.remove(file);
    }

    /** commit all the file in the staging area, remove the staging area, and return a commit */
    public Commit commit(String message){
        if (staging_area.isEmpty() && this.file_for_removal.isEmpty()){
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        if (message == ""){
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        HashMap<String, String> new_blobs = copy_blob(head.blobs());
        for (String fileName : staging_area.keySet()) {
            String hashcode = Utils.sha1(staging_area.get(fileName));
            new_blobs.put(fileName, hashcode);
            File file = Utils.join(Repository.BLOBS_DIR,hashcode);
            Utils.writeContents(file, staging_area.get(fileName));
        }
        for (String key : file_for_removal){
            new_blobs.remove(key);
        }
        Commit new_commit = new Commit(message,new Date(),Utils.sha1(Utils.serialize(head)),new_blobs);
        new_commit.length_increment(head.length() + 1);
        this.head = new_commit;
        staging_area.clear();
        file_for_removal.clear();
        return new_commit;
    }

    public Commit mergeCommit(String message, String parent1, String parent2){
        Commit mergeCommit = commit(message);
        mergeCommit.parent1(parent1);
        mergeCommit.parent2(parent2);
        return mergeCommit;
    }

    /** remove a file from the staging area */
    public void unstage(String file){
        staging_area.remove(file);
    }

    /** stage a file for removal */
    public void stage_for_removal(String file){
        this.file_for_removal.addLast(file);
    }

    /** if the staging area has the file */
    public boolean exist(String file){
        return staging_area.containsKey(file);
    }
    /** private helper method of making a copy of blob */
    private HashMap<String,String> copy_blob(Map<String,String> old_blobs){
        HashMap<String, String> new_blobs = new HashMap<>();
        for (String fileName : old_blobs.keySet()) {
            new_blobs.put(fileName, old_blobs.get(fileName));
        }
        return new_blobs;
    }

    /** return the staging are of the Stage.class */
    public HashMap<String,String> staging_area(){
        return staging_area;
    }
    /** return the removal file */
    public LinkedList<String> remove_file(){
        return file_for_removal;
    }

    /** clear the staged for commit and removal */
    public void clear(){
        file_for_removal.clear();
        staging_area.clear();
    }
}
