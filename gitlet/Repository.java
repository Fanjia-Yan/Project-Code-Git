package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Fanjia
 */
public class Repository implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    /** The tree directory */
    public static final File TREES_DIR = Utils.join(GITLET_DIR,"trees");
    /** The blob directory */
    public static final File BLOBS_DIR = Utils.join(GITLET_DIR,"blobs");
    /** The commit directory */
    public static final File COMMITS_DIR = Utils.join(GITLET_DIR,"commits");
    /** The stage directory */
    public static final File STAGES_DIR = Utils.join(GITLET_DIR,"stages");
    /** Linkedlist of commit */
    private LinkedList<String> commit_list;
    /** HashMap of {name of the branch : commitID} */
    private HashMap<String,String> branch_map;
    private String head;
    private String branch;
    private Stage staging_area;
    private boolean after_reset;

    /** constructor of Repository */
    public Repository(){
        commit_list = new LinkedList<String>();
        branch_map = new HashMap<>();
        after_reset = false;
    }

    /** start with one commit, single branch master, return the commit */
    public Commit init(){
        if(GITLET_DIR.exists()){
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return null;
        }
        /** get all the directory set up */
        COMMITS_DIR.mkdirs();
        BLOBS_DIR.mkdirs();
        STAGES_DIR.mkdirs();

        /** first commit **/
        Commit first_commit = new Commit("initial commit", new Date(0), null, new HashMap<>());
        String commitID = first_commit.sha();
        save_commit(first_commit,commitID);

        /** initialize head and branches */
        head = commitID;
        commit_list.addLast(commitID);
        branch = "master";
        branch_map.put("master", commitID);

        /** set up current stage */
        staging_area = new Stage();
        staging_area.set_head(first_commit);

        return first_commit;
    }

    /** add the file to the staging area, most code in Stage.java */
    public void add(String file){
        staging_area.add(file);
    }

    /** know what file is in the staging area and take a snapshot of all of them, remember it as commit */
    public Commit commit(String message){
        Commit make_commit = staging_area.commit(message);
        head = make_commit.sha();
        commit_list.add(head);
        branch_map.put(branch,head);
        save_commit(make_commit,head);
        return make_commit;
    }

    /** use the linkedlist of commitlist to print all the commit backward */
    public void log(){
        Commit head_commit = read_commit(head);
        while (head_commit != null) {
            print_commit(head_commit);
            if (head_commit.parent() != null) {
                head_commit = read_commit(head_commit.parent());
            } else {
                head_commit = null;
            }
        }
    }

    /**  checkout commitID -- file, going back to commitID commit */
    public void checkout(String commitID, String file){
        File current_file = Utils.join(CWD,file);
        File current_commit_file = Utils.join(COMMITS_DIR,commitID);
        if (commitID.length() >= 6 && commitID.length() < 24){
            List<String> commitID_list = Utils.plainFilenamesIn(COMMITS_DIR);
            for (String commit :commitID_list){
                boolean flag = true;
                char[] commit_array = commit.toCharArray();
                char[] commitID_array = commitID.toCharArray();
                for (int i = 0; i < commitID.length(); i = i+ 1){
                    if (commit_array[i] != commitID_array[i]){
                        flag = false;
                        break;
                    }
                }
                if (flag){
                    current_commit_file = Utils.join(COMMITS_DIR,commit);
                }
            }
        }
        if (!current_commit_file.exists()){
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit most_recent_commit = Utils.readObject(current_commit_file,Commit.class);
        if (most_recent_commit.blobs().get(file) == null){
            System.out.println("File does not exist in that commit.");
            return;
        }

        Map<String,String> blob = most_recent_commit.blobs();
        String blob_id = blob.get(file);
        File blobfile = Utils.join(BLOBS_DIR,blob_id);
        byte[] oldContent = Utils.readContents(blobfile);
        Utils.writeContents(current_file,oldContent);
    }

    /** checkout -- file going back to the most recent commit */
    public void checkout(String file){
        checkout(head,file);
    }

    /** checkout branch put all the file from head commit of the branch to CWD */
    public void checkout_branch(String branch){
        if (!branch_map.containsKey(branch)){
            System.out.println("No such branch exists.");
            return;
        }
        if (this.branch.equals(branch)){
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String head_commit_id = branch_map.get(branch);
        Commit head_commit = read_commit(head_commit_id);
        List<String> untracked_file = untrack_list(head_commit);
        if (!untracked_file.isEmpty()) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            return;
        }
        for(String file : head_commit.blobs().keySet()){
            checkout(head_commit_id,file);
        }

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!head_commit.blobs().containsKey(fileName) && read_commit(this.head).blobs().containsKey(fileName)) {
                Utils.restrictedDelete(Utils.join(CWD,fileName));
            }
        }
        staging_area.clear();
        staging_area.set_head(head_commit);
        head = head_commit_id;
        this.branch = branch;
    }

    /** remove or unstage a file */
    public void remove(String file){
        File remove_file = Utils.join(CWD,file);
        Commit head_commit = read_commit(head);
        if (!staging_area.exist(file) && !head_commit.blobs().containsKey(file)){
            System.out.println("No reason to remove the file.");
        }
        if(staging_area.exist(file)){
            staging_area.unstage(file);
        }
        if(head_commit.blobs().containsKey(file) && remove_file.exists()){
            Utils.restrictedDelete(remove_file);
            staging_area.stage_for_removal(file);
        }
    }

    /** display all the log information */
    public void global_log(){
        List<String> commit_history = Utils.plainFilenamesIn(COMMITS_DIR);
        for (String commitID :commit_history){
            File commit_file = Utils.join(COMMITS_DIR,commitID);
            Commit commit_item = Utils.readObject(commit_file,Commit.class);
            print_commit(commit_item);
        }
    }

    /** find commit(s) with certain message */
    public void find(String message){
        List<String> commit_history = Utils.plainFilenamesIn(COMMITS_DIR);
        boolean flag = false;
        for (String commitID : commit_history){
            File commit_file = Utils.join(COMMITS_DIR,commitID);
            Commit commit_item = Utils.readObject(commit_file,Commit.class);
            if(commit_item.message().equals(message)){
                System.out.println(commit_item.sha());
                flag = true;
            }
        }
        if (flag == false){
            System.out.println("Found no commit with that message.");
        }
    }

    /** print out the current status of the git */
    public void status(){
        /** print branches */
        System.out.println("=== Branches ===");
        System.out.print("*");
        System.out.println(branch);
        for(String iter_branch : branch_map.keySet()){
            if (!branch.equals(iter_branch)) {
                System.out.println(iter_branch);
            }
        }

        /** print staged file */
        HashMap<String,String> staged_filed = staging_area.staging_area();
        System.out.println();
        System.out.println("=== Staged Files ===");
        for(String file : staged_filed.keySet()){
            System.out.println(file);
        }

        /** unstage unix remove file */
        if (!after_reset) {
            Commit head_commit = read_commit(head);
            for (String file : head_commit.blobs().keySet()) {
                File tracked_file = Utils.join(CWD, file);
                if (!tracked_file.exists()) {
                    if (staging_area.staging_area().containsKey(file)) {
                        staging_area.unstage(file);
                    } else if (!staging_area.remove_file().contains(file)) {
                        staging_area.stage_for_removal(file);
//                    System.out.println(file);
                    }
                }
            }
        }

        /** print removed file */
        LinkedList<String> removed_file = staging_area.remove_file();
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String file :removed_file){
            System.out.println(file);
        }

        /**print modified file */
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        /** print untracked file */
        System.out.println();
        System.out.println("=== Untracked Files ===");

        System.out.println();
        after_reset = false;
    }

    /** create a new branch if the branch doesn't exist, and point it to head commit */
    public void create_branch(String branch_name){
        if (branch_map.containsKey(branch_name)){
            System.out.println("A branch with that name already exists.");
            return;
        }
        branch_map.put(branch_name,head);
    }

    /** remove an non-current existing branch */
    public void rm_branch(String branch_name){
        if (!branch_map.containsKey(branch_name)){
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branch_name.equals(branch)){
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch_map.remove(branch_name);
    }

    /** reset based on the commit ID */
    public void reset(String CommitID){
        List commit_history = Utils.plainFilenamesIn(COMMITS_DIR);
        if (!commit_history.contains(CommitID)){
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit reset_commit = read_commit(CommitID);
        Commit curr_commit = read_commit(head);
        List<String> untracked_file = untrack_list(reset_commit);
        if (!untracked_file.isEmpty()){
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        /** checkout all the file with given commit */
        for (String file : reset_commit.blobs().keySet()){
            checkout(CommitID,file);
        }

        /** Removes tracked files that are not present in that commit */
        for (String file : curr_commit.blobs().keySet()){
            if (!reset_commit.blobs().keySet().contains(file)){
                remove(file);
            }
        }

        /** move the current branch head to commit node */
        branch_map.replace(branch,CommitID);
        head = CommitID;
        staging_area.clear();
        after_reset = true;
    }

    /** merge the two branch from the split point */
    public void merge(String other_branch){
        /** check for any failure case */
        if (!staging_area.staging_area().isEmpty() && !staging_area.remove_file().isEmpty()){
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branch_map.containsKey(other_branch)){
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branch.equals(other_branch)){
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        if (!untrack_list(read_commit(head)).isEmpty()){
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            return;
        }

        Commit head_commit = read_commit(head);
        Commit other_head_commit = read_commit(branch_map.get(other_branch));
        Commit split_commit = null;

        /** find the split point for the two commit */
        Commit pointer_head = head_commit;
        Commit pointer_other = other_head_commit;
        while (pointer_head.length() != pointer_other.length()){
            if (pointer_head.length() > pointer_other.length()){
                pointer_head = read_commit(pointer_head.parent());
            }else{
                pointer_other = read_commit(pointer_other.parent());
            }
        }

        while (pointer_head.parent() != null){
            if (pointer_head.message().equals(pointer_other.message())){
                split_commit = pointer_head;
                break;
            }
            pointer_head = read_commit(pointer_head.parent());
            pointer_other = read_commit(pointer_other.parent());
        }

        /** put all blobs into a Set */
        Map<String, String> other_blob = other_head_commit.blobs();
        Map<String, String> split_blob = split_commit.blobs();
        Map<String, String> head_blob = head_commit.blobs();

        Set<String> blob_set = new HashSet<>();
        blob_set.addAll(other_blob.keySet());
        blob_set.addAll(split_blob.keySet());
        blob_set.addAll(head_blob.keySet());

        /** put stuff into the new merge blob */
        Map<String,String> merge_blob = new HashMap<>();
        for (String fileName : blob_set) {
            String other_Blob = other_blob.getOrDefault(fileName, "");
            String split_Blob = split_blob.getOrDefault(fileName, "");
            String head_Blob = head_blob.getOrDefault(fileName, "");
            if (other_Blob.equals(split_Blob) && head_Blob.equals(other_Blob)){
                merge_blob.put(fileName, head_Blob);
            }
            else if (other_Blob.equals(head_Blob) && !other_Blob.equals(split_Blob)) {
                merge_blob.put(fileName, head_Blob);
            }
            else if (!other_Blob.equals(split_Blob) && split_Blob.equals(head_Blob)) {
                merge_blob.put(fileName, other_Blob);
                File givenFile = Utils.join(BLOBS_DIR,other_Blob);
                File curFile = Utils.join(CWD,fileName);
                if (givenFile.exists() && other_Blob.length() > 0){
                    String contents = Utils.readContentsAsString(givenFile);
                    Utils.writeContents(curFile, contents);
                    staging_area.add(fileName);
                } else {
                    Utils.restrictedDelete(curFile);
                    staging_area.stage_for_removal(fileName);
                }
            }
            else if (!other_Blob.equals(head_Blob) && !other_Blob.equals(split_Blob) && !head_Blob.equals(split_Blob)){
                File givenFile = Utils.join(BLOBS_DIR,other_Blob);
                File headFile = Utils.join(BLOBS_DIR,head_Blob);
                String givenContent = "";
                String headContent = "";
                if (givenFile.exists() && other_Blob.length() > 0) {
                    givenContent = Utils.readContentsAsString(givenFile);
                }
                if (headFile.exists() && head_Blob.length() > 0) {
                    headContent = Utils.readContentsAsString(headFile);
                }

                File current_file = Utils.join(CWD,fileName);
                Utils.writeContents(current_file, "<<<<<<<< HEAD\n" + headContent +
                        "===========\n" + givenContent + ">>>>>>>>");
                staging_area.add(fileName);
            }
        }

        String message = String.format("Merged %s into %s", other_branch, branch);
        Commit mergeCommit = staging_area.mergeCommit(message, head, branch_map.get(other_branch));
        String merge_commit_id = Utils.sha1(Utils.serialize(mergeCommit));
        commit_list.add(merge_commit_id);
        File mergeCommitFile = Utils.join(COMMITS_DIR,merge_commit_id);
        Utils.writeObject(mergeCommitFile, mergeCommit);
        head = merge_commit_id;
        branch_map.put(branch, merge_commit_id);
    }


    /** print the log information give the commitID */
    private Commit read_commit(String commitID){
        File commit_location = Utils.join(COMMITS_DIR,commitID);
        return Utils.readObject(commit_location,Commit.class);
    }

    /** print the commit statement in a log form */
    private void print_commit(Commit commit){
        String new_date = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(commit.date());
        System.out.println("===");
        System.out.println("commit " + commit.sha());
        System.out.println("Date: " + new_date);
        System.out.println(commit.message());
        System.out.println();
    }

    /** private method of saving commit to the commit directory */
    private void save_commit(Commit commit, String commitID){
        File commit_location = Utils.join(COMMITS_DIR,commitID);
        Utils.writeObject(commit_location,commit);
    }

    /** return a list of file that is untracked by the commit */
    private List<String> untrack_list(Commit commit){
        List<String> untrack_file = new ArrayList<String>();
        List<String> CWD_list = Utils.plainFilenamesIn(CWD);
        Map<String,String> commit_blob = commit.blobs();
        Map<String,String> curr_commit_blob = read_commit(head).blobs();
        for (String file : commit_blob.keySet()){
            String file_content_loc = commit_blob.get(file);
            File blob_file = Utils.join(BLOBS_DIR,file_content_loc);
            String blob_content = Utils.readContentsAsString(blob_file);
            File exist_file = Utils.join(CWD,file);
            if (!exist_file.exists()){
                continue;
            }
            String exist_content = Utils.readContentsAsString(exist_file);
            if (!blob_content.equals(exist_content) && !curr_commit_blob.containsKey(file)){
                untrack_file.add(file);
            }
        }
        return untrack_file;
    }
}
