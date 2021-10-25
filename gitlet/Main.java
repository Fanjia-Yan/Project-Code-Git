package gitlet;

import java.io.File;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Fanjia
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        /** check whether the argument is parsed in correctly */
        int input_length = args.length;
        if (input_length == 0) {
            System.out.println("Please enter a command");
            System.exit(0);
        }
        String firstArg = args[0];
        Repository tree;

        /** if not init, check whether the directory has been initialized or not **/
        if (firstArg.equals("init")) {
            init_main();
        } else {
            File tree_file = Repository.TREES_DIR;
            if (!tree_file.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            tree = Utils.readObject(tree_file, Repository.class);
            switch (firstArg) {
                case "add":
                    add_main(tree, args);
                    break;
                case "commit":
                    commit_main(tree, args);
                    break;
                case "log":
                    log_main(tree,args);
                    break;
                case "checkout":
                    checkout_main(tree,args);
                    break;
                case "rm":
                    rm_main(tree,args);
                    break;
                case "global-log":
                    global_log_main(tree,args);
                    break;
                case "find":
                    find_main(tree,args);
                    break;
                case "status":
                    status_main(tree,args);
                    break;
                case "branch":
                    branch_main(tree,args);
                    break;
                case "rm-branch":
                    rm_branch_main(tree,args);
                    break;
                case "reset":
                    reset_main(tree,args);
                    break;
                case "merge":
                    merge_main(tree,args);
                    break;
                default:
                    System.out.println("No command with that name exists.");
            }
            save_tree(tree);
        }
    }

    /** initialize the repository, and save it under the repo */
    public static void init_main(){
        Repository tree = new Repository();
        tree.init();
        save_tree(tree);
    }

    /** add file to the staging area */
    public static void add_main(Repository tree, String[] args){
        if(args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.add(args[1]);
    }

    /** commit the files that are in the staging area */
    public static void commit_main(Repository tree,String[] args){
        if(args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        if (args[1].isBlank()){
            System.out.println("Please enter a commit message.");
            return;
        }
        tree.commit(args[1]);
    }

    /** print out the log of previous commits */
    public static void log_main(Repository tree,String[] args){
        if (args.length != 1){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.log();
    }

    /** check-out go to the file depends on what our inputs are */
    /** checkout -- file, checkout 1234 -- file */
    public static void checkout_main(Repository tree,String[] args){
        if (args.length ==2 ){
            tree.checkout_branch(args[1]);
        }else if (args.length == 3 ){
            tree.checkout(args[2]);
        }else if (args.length == 4 && args[2].equals("--")) {
            tree.checkout(args[1],args[3]);
        }else{
            System.out.println("Incorrect operands.");
        }
    }

    /** Unstage the file if its in the stage, If the file is
     *tracked in the current commit, stage it for removal and
     * remove the file from the working directory*/
    public static void rm_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.remove(args[1]);
    }

    /** displays information about all commits ever made */
    public static void global_log_main(Repository tree, String[] args){
        if (args.length != 1){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.global_log();
    }

    /** Prints out the ids of all commits that have the given commit message, one per line */
    public static void find_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.find(args[1]);
    }

    /** print out the status of current gitlet */
    public static void status_main(Repository tree, String[] args){
        if (args.length != 1){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.status();
    }

    /** create the branch with argument name */
    public static void branch_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.create_branch(args[1]);
    }

    /** remove a non-current existing branch */
    public static void rm_branch_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.rm_branch(args[1]);
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     */
    public static void reset_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
            return;
        }
        tree.reset(args[1]);
    }

    /** the notorious merge */
    public static void merge_main(Repository tree, String[] args){
        if (args.length != 2){
            System.out.println("Incorrect operands.");
        }
        tree.merge(args[1]);
    }
    /** save the tree to trees_dir directory */
    private static void save_tree(Repository tree){
        File repo_file = Utils.join(Repository.TREES_DIR);
        Utils.writeObject(repo_file, tree);
    }
}
