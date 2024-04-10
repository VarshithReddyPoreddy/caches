import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
public class sim_cache{
    //  Inputs for the User
    static int L1_Block_size;
    static int L1_Byte_size;
    static int L1_Assoc;
    static int L2_Byte_size;
    static int L2_Assoc;
    static String Replacement_algo;
    static String Inclusivity;
    static String file_name;

    static int l1_reads,l1_read_misses,l1_writes,l1_write_misses,l1_write_backs = 0;
    static int l2_reads,l2_read_misses,l2_writes,l2_write_misses,l2_write_backs = 0;
    static int memory_writeback;
    static int L1_sets, L2_sets;
    static double L1_index_bits,L1_offset_bits,L1_tag_bit, L2_index_bits, L2_offset_bits, L2_tag_bit;
    static LinkedList<Integer>[] L1_SET_LRU_req_list,L1_SET_FIFO_req_list,  L2_SET_LRU_req_list,L2_SET_FIFO_req_list;
    static int [][][] L1_Cache_data, L2_Cache_data;
    static int L2_Block_size;

    public static void main(String[] args) {
        L1_Block_size = Integer.parseInt(args[0]);
        L1_Byte_size = Integer.parseInt(args[1]);
        L1_Assoc = Integer.parseInt(args[2]);
        L2_Byte_size = Integer.parseInt(args[3]);
        L2_Assoc = Integer.parseInt(args[4]);
        if(Integer.parseInt(args[5]) == 0){
            Replacement_algo = "LRU";
        }
        else {
            Replacement_algo = "FIFO";
        }
        if(Integer.parseInt(args[6]) == 1) {
            Inclusivity = "inclusive";
        }else {
            Inclusivity = "non-inclusive";
        }
        file_name = args[7];
        L2_Block_size = L1_Block_size;

        L1_sets = L1_Byte_size /(L1_Block_size * L1_Assoc);
        L1_index_bits = Math.log(L1_sets) / Math.log(2);
        L1_offset_bits = Math.log(L1_Block_size) / Math.log(2);
        L1_tag_bit = 32 - L1_index_bits - L1_offset_bits;
        L1_Cache_data = new int[L1_sets][][];
        L1_SET_LRU_req_list = new LinkedList[L1_sets];
        L1_SET_FIFO_req_list = new LinkedList[L1_sets];
        if (L2_Byte_size > 0){
            L2_sets = L2_Byte_size /(L2_Block_size * L2_Assoc);
            L2_index_bits = Math.log(L2_sets) / Math.log(2);
            L2_offset_bits = Math.log(L2_Block_size) / Math.log(2);
            L2_tag_bit = 32 - L2_index_bits - L2_offset_bits;
            L2_SET_LRU_req_list = new LinkedList[L2_sets];
            L2_SET_FIFO_req_list = new LinkedList[L2_sets];
            L2_Cache_data = new int[L2_sets][][];
        }
        for (int i = 0; i < L1_sets; i++) {
            L1_SET_LRU_req_list[i] = new LinkedList();
            L1_SET_FIFO_req_list[i] = new LinkedList();
        }
        if (L2_Byte_size > 0) {
            for (int j = 0; j < L2_sets; j++) {
                L2_SET_LRU_req_list[j] = new LinkedList();
                L2_SET_FIFO_req_list[j] = new LinkedList();
            }
        }
        L1_Cache_data = Cache_initiate(L1_sets,L1_Assoc);
        if (L2_Byte_size > 0) {
            L2_Cache_data = Cache_initiate(L2_sets,L2_Assoc);
        }

        String filePath = file_name; // Provide the path to your input file.
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            int count = 1;
            while (line != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    String operation = parts[0];
                    String address = parts[1];
                    //System.out.println("----------------------------------------");
                    // Call the read_write_block function based on the operation and address.
                    if (operation.equals("w")) {
                        //System.out.println("# "+count+" : write " + address);
                        L1_read_write_block("W", address);
                    } else if (operation.equals("r")) {
                        //System.out.println("# "+count+" : read " + address);
                        L1_read_write_block("R", address);
                    }
                    line = reader.readLine();
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE:             " + L1_Block_size);
        System.out.println("L1_SIZE:               " + L1_Byte_size);
        System.out.println("L1_ASSOC:              " + L1_Assoc);
        System.out.println("L2_SIZE:               " + L2_Byte_size);
        System.out.println("L2_ASSOC:              " + L2_Assoc);
        System.out.println("REPLACEMENT POLICY:    " + Replacement_algo);
        System.out.println("INCLUSION PROPERTY:    " + Inclusivity);
        System.out.println("trace_file:            " + file_name);

        System.out.print("===== L1 contents =====");
        display_cache(L1_Cache_data, L1_sets, L1_Assoc);
        if (L2_Byte_size > 0) {
            System.out.println();
            System.out.print("===== L2 contents =====");
            display_cache(L2_Cache_data, L2_sets, L2_Assoc);
        }
        int traffic;
        double l1_miss_rate,l2_miss_rate;
        if (L2_Byte_size > 0) {
            traffic = l2_read_misses + l2_write_misses + l2_write_backs + memory_writeback;
            l2_miss_rate = (double) l2_read_misses / (double) l2_reads;
        }
        else {
            traffic = l1_read_misses + l1_write_misses + l1_write_backs;
            l2_miss_rate = (double) 0;
        }
        l1_miss_rate = (double) (l1_read_misses + l1_write_misses)/ (double) (l1_reads + l1_writes);

        System.out.println();
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads:        " + l1_reads);
        System.out.println("b. number of L1 read misses:  " + l1_read_misses);
        System.out.println("c. number of L1 writes:       " + l1_writes);
        System.out.println("d. number of L1 write misses: " + l1_write_misses);
        System.out.println("e. L1 miss rate:              " + String.format("%.6f", l1_miss_rate));
        System.out.println("f. number of L1 writebacks:   " + l1_write_backs);
        System.out.println("g. number of L2 reads:        " + l2_reads);
        System.out.println("h. number of L2 read misses:  " + l2_read_misses);
        System.out.println("i. number of L2 writes:       " + l2_writes);
        System.out.println("j. number of L2 write misses: " + l2_write_misses);
        System.out.println("k. L2 miss rate:              " + String.format("%.6f", l2_miss_rate));
        System.out.println("l. number of L2 writebacks:   " + l2_write_backs);
        System.out.println("m. total memory traffic:      " + traffic);
    }

    static int[][][] Cache_initiate(int sets , int assoc){
        int[][][] Cache_data = new int[sets][][];
        for (int i = 0; i < sets; i++) {
            Cache_data[i] = new int[assoc][];
            for (int j = 0; j < assoc; j++) {
//                                              Tag,ISValid,ISDirty,Address
                Cache_data[i][j] = new int[]{0, 0, 0, 0};
            }
        }
        return Cache_data;
    }
    static void display_cache(int[][][] cache_data, int sets, int assoc){
        int tot_length = String.valueOf(sets).length();
        for (int i = 0; i < sets; i++){
            System.out.println();
            int set_length = String.valueOf(i).length();
            set_length = tot_length - set_length;
            for (int j = 0; j< assoc; j++){
                if(j==0){
                    System.out.print("Set     " + i + ":" );
                    for (int k = 0; k < set_length; k++) {
                        System.out.print(" ");
                    }
                    System.out.print("    ");
                }
                System.out.print(Integer.toHexString(cache_data[i][j][0]));
                if (cache_data[i][j][2] == 1){
                    System.out.print(" D   " );
                } else { System.out.print("     " ); }
            }
        }
    }
    static boolean ispresent_read_cache(int[][][] Cache_data,int to_find_tag, int to_find_index, int Assoc){
        for (int i = 0; i < Assoc; i++) {
            int curr_tag = Cache_data[to_find_index][i][0];
            if (to_find_tag == curr_tag) {
                return true;
            }
        }
        return false;
    }
    static boolean ispresent_write_cache(int[][][] Cache_data,int to_find_tag, int to_find_index, int Assoc, String address, String cache){
        int decimalValue = Integer.parseInt(address, 16);
        for (int i = 0; i < Assoc; i++) {
            int curr_tag = Cache_data[to_find_index][i][0];
            if (to_find_tag == curr_tag) {
                if (cache == "L1"){
                    L1_Cache_data[to_find_index][i] = new int[]{curr_tag, 0, 1,decimalValue};
                } else {
                    L2_Cache_data[to_find_index][i] = new int[]{curr_tag, 0, 1,decimalValue};
                }
                return true;
            }
        }
        return false;
    }
    static boolean isempty_allocate_cache(int[][][] Cache_data,int to_allocate_tag, int to_find_index, int Assoc, String address){
        int decimalValue = Integer.parseInt(address, 16);
        for (int i = 0; i < Assoc; i++) {
            int curr_tag = Cache_data[to_find_index][i][0];
            if (curr_tag == 0) {
                Cache_data[to_find_index][i] = new int[]{to_allocate_tag, 0, 0,decimalValue};
                return true;
            }
        }
        return false;
    }
    public static String replaceLastCharWithZero(String input) {
        return input != null && !input.isEmpty() ? new StringBuilder(input).replace(input.length() - 1, input.length(), "0").toString() : input;
    }

    static void L1_read_write_block(String opr, String address){
        int decimalValue = Integer.parseInt(address, 16);
        String binary_number = Integer.toBinaryString(decimalValue);

//      Getting the Tag value
        String L1_tag_value = binary_number.substring(0,(int) (binary_number.length() -  (L1_index_bits + L1_offset_bits)));
        int L1_tag_value_decimal = Integer.parseInt(L1_tag_value, 2);
//      Getting the index Number
        int L1_index_value_decimal;
        if (L1_index_bits != 0) {
            String L1_index_value = binary_number.substring((int) (binary_number.length() - (L1_index_bits + L1_offset_bits)), (int) (binary_number.length() - L1_offset_bits));
            L1_index_value_decimal = Integer.parseInt(L1_index_value, 2);
        } else {
            L1_index_value_decimal = 0;
        }
//      <----------------------->
//      For Read Request
//      <----------------------->
        if (opr == "R") {
            if(L1_Assoc > 0) {
                l1_reads += 1;
                //System.out.println("L1 read : " + replaceLastCharWithZero(address) + " (tag " + Integer.toHexString(L1_tag_value_decimal) +", index " + L1_index_value_decimal + ")");
                boolean is_present_cache = ispresent_read_cache(L1_Cache_data,L1_tag_value_decimal,L1_index_value_decimal,L1_Assoc);
                if (is_present_cache) {
                    //System.out.println("L1 hit");
                }
                else {
                    l1_read_misses += 1;
                    //System.out.println("L1 miss");
                    L1_allocate_block(address,"R",L1_tag_value_decimal,L1_index_value_decimal);
                }
                L1_LRU_Replacement(address, L1_tag_value_decimal, L1_index_value_decimal);
            }
        }
//      <----------------------->
//      For Write Request
//      <----------------------->
        else if (opr == "W") {
            if (L1_Assoc > 0){
                l1_writes += 1;
                //System.out.println("L1 write : " + replaceLastCharWithZero(address) + " (tag " + Integer.toHexString(L1_tag_value_decimal) +", index " + L1_index_value_decimal + ")");
                boolean is_present_cache = ispresent_write_cache(L1_Cache_data,L1_tag_value_decimal,L1_index_value_decimal,L1_Assoc,address,"L1");
                if (is_present_cache) {
                    //System.out.println("L1 hit");
                }
                else {
                    l1_write_misses += 1;
                    //System.out.println("L1 miss");
                    L1_allocate_block(address,"W", L1_tag_value_decimal, L1_index_value_decimal);
                    is_present_cache = ispresent_write_cache(L1_Cache_data,L1_tag_value_decimal,L1_index_value_decimal,L1_Assoc,address,"L1");
                }
                L1_LRU_Replacement(address,L1_tag_value_decimal,L1_index_value_decimal);
                //System.out.println("L1 set dirty");
            }
        }
    }
    static void L1_allocate_block(String address,String Opr,int tag_decimal,int index){
        int decimalValue = Integer.parseInt(address, 16);
        L1_FIFO_Replacement(address,tag_decimal,index);
        if (L1_Assoc > 1){
            boolean cache_set_full = isempty_allocate_cache(L1_Cache_data,tag_decimal,index,L1_Assoc,address);
            if (!cache_set_full) {
                L1_delete_block(address,index,tag_decimal);
            } else {
                //System.out.println("L1 victim: none");
            }
        } else if (L1_Assoc == 1){
            if (L1_Cache_data[index][0][0] != 0) {
                L1_delete_block(address,index,tag_decimal);
            } else {
                //System.out.println("L1 victim: none");
            }
            L1_Cache_data[index][0] = new int[]{tag_decimal, 0, 0, decimalValue};
        }
        if (L2_Byte_size > 0){
            L2_read_write_block(address,"R");
        }
    }
    static void L1_delete_block(String address,int index_decimal, int tag_decimal){
        int decimalValue = Integer.parseInt(address, 16);
        if (L1_Assoc > 1) {
            int to_rmw_tag_decimal;
            if (Replacement_algo == "LRU") {
                to_rmw_tag_decimal = L1_SET_LRU_req_list[index_decimal].getLast();
                L1_SET_LRU_req_list[index_decimal].removeLast();
            } else {
                to_rmw_tag_decimal = L1_SET_FIFO_req_list[index_decimal].getLast();
                L1_SET_FIFO_req_list[index_decimal].removeLast();
            }
            for (int k = 0; k < L1_Assoc; k++) {
                if (L1_Cache_data[index_decimal][k][0] == to_rmw_tag_decimal) {
                    if (L1_Cache_data[index_decimal][k][2] == 1) {
                        //System.out.println("L1 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][k][3])) + " (tag " + Integer.toHexString(to_rmw_tag_decimal) + ", index " + index_decimal + ", dirty)");
                        l1_write_backs += 1;
                        if (L2_Byte_size > 0){
                            String hexadecimal = Integer.toHexString(L1_Cache_data[index_decimal][k][3]);
                            L2_read_write_block(hexadecimal, "W");
                        }
                    } else{
                        //System.out.println("L1 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][k][3])) + " (tag " + Integer.toHexString(to_rmw_tag_decimal) + ", index " + index_decimal + ", clean)");
                    }
//                  Allocate New
                    L1_Cache_data[index_decimal][k] = new int[]{tag_decimal, 0, 0, decimalValue};
                }
            }
        } else if (L1_Assoc == 1) {
            if (L1_Cache_data[index_decimal][0][2] == 1) {
                l1_write_backs += 1;
                //System.out.println("L1 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][0][3])) + " (tag " + Integer.toHexString(L1_Cache_data[index_decimal][0][0]) + ", index " + index_decimal + ", dirty)");
                String hexadecimal = Integer.toHexString(L1_Cache_data[index_decimal][0][3]);
                if (L2_Byte_size > 0){
                    L2_read_write_block(hexadecimal, "W");
                }
            } else {
                //System.out.println("L1 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][0][3])) + " (tag " + Integer.toHexString(L1_Cache_data[index_decimal][0][0]) + ", index " + index_decimal + ", clean)");
            }
        }
    }
    static void L1_LRU_Replacement(String address,int tag_decimal, int index_decimal){
        //System.out.println("L1 update LRU");
        int decimal_address = Integer.parseInt(address, 16);
        if (L1_Assoc > 1) {
            if (L1_SET_LRU_req_list[index_decimal].contains(tag_decimal)){
                L1_SET_LRU_req_list[index_decimal].remove((Integer) tag_decimal);
            } else if (L1_SET_LRU_req_list[index_decimal].size() == L1_Assoc){
                L1_SET_LRU_req_list[index_decimal].removeLast();
//          Remove the same from the CacheData
            }
            L1_SET_LRU_req_list[index_decimal].addFirst(tag_decimal);
        }
    }
    static void L1_FIFO_Replacement(String address,int tag_decimal, int index_decimal) {
        int decimal_address = Integer.parseInt(address, 16);
        if (L1_Assoc > 1) {
            if (!L1_SET_FIFO_req_list[index_decimal].contains(tag_decimal)) {
                L1_SET_FIFO_req_list[index_decimal].addFirst(tag_decimal);
            }
        }
    }

    static void L2_read_write_block(String address, String opr){
        int decimalValue = Integer.parseInt(address, 16);
        String binary_number = Integer.toBinaryString(decimalValue);

        String L2_tag_value = binary_number.substring(0,(int) (binary_number.length() -  (L2_index_bits + L2_offset_bits)));
        int L2_tag_value_decimal = Integer.parseInt(L2_tag_value, 2);
        int L2_index_value_decimal;
        if (L2_index_bits != 0) {
            String L2_index_value = binary_number.substring((int) (binary_number.length() - (L2_index_bits + L2_offset_bits)), (int) (binary_number.length() - L2_offset_bits));
            L2_index_value_decimal = Integer.parseInt(L2_index_value, 2);
        } else{
            L2_index_value_decimal = 0;
        }
        if (opr == "R") {
            if (L2_Assoc > 0) {
                l2_reads += 1;
                //System.out.println("L2 read : " + replaceLastCharWithZero(address) + " (tag " + Integer.toHexString(L2_tag_value_decimal) +", index " + L2_index_value_decimal + ")");
                boolean is_present_cache = ispresent_read_cache(L2_Cache_data,L2_tag_value_decimal,L2_index_value_decimal,L2_Assoc);
                if (is_present_cache){
                   // System.out.println("L2 hit");
                }
                else {
                    l2_read_misses += 1;
                    //System.out.println("L2 miss");
                    L2_allocate_block(address,opr,L2_tag_value_decimal,L2_index_value_decimal);
                }
                L2_LRU_Replacement(address,L2_tag_value_decimal,L2_index_value_decimal);
            }
        } else if (opr == "W"){
            if (L2_Assoc > 0) {
                l2_writes += 1;
                //System.out.println("L2 write : " + replaceLastCharWithZero(address) + " (tag " + Integer.toHexString(L2_tag_value_decimal) +", index " + L2_index_value_decimal + ")");
                boolean is_present_cache = ispresent_write_cache(L2_Cache_data,L2_tag_value_decimal,L2_index_value_decimal,L2_Assoc,address,"L2");
                if (is_present_cache){
                   // System.out.println("L2 hit");
                }
                else {
                    l2_write_misses += 1;
                    //System.out.println("L2 miss");
                    L2_allocate_block(address,opr,L2_tag_value_decimal,L2_index_value_decimal);
                }
                is_present_cache = ispresent_write_cache(L2_Cache_data,L2_tag_value_decimal,L2_index_value_decimal,L2_Assoc,address,"L2");
                L2_LRU_Replacement(address,L2_tag_value_decimal,L2_index_value_decimal);
                //System.out.println("L2 set dirty");
            }
        }
    }
    static void L2_allocate_block(String address,String Opr,int tag_decimal,int index){
        int decimalValue = Integer.parseInt(address, 16);
//        Initiate FIFO
        L2_FIFO_Replacement(address, tag_decimal, index);
        if (L2_Assoc > 1) {
            boolean cache_set_full = isempty_allocate_cache(L2_Cache_data,tag_decimal,index,L2_Assoc,address);
            if (!cache_set_full) {
                L2_delete_block( address, tag_decimal, index);
            } else {
                //System.out.println("L2 victim: none");
            }
        } else if (L2_Assoc == 1) {
            if (L2_Cache_data[index][0][0] != 0) {
                L2_delete_block( address, tag_decimal, index);
            } else {
                //System.out.println("L2 victim: none");
            }
            L2_Cache_data[index][0] = new int[] {tag_decimal,0,0,decimalValue};
        }
    }
    static void L2_delete_block(String address,int tag_decimal,int index_decimal){
        int decimalValue = Integer.parseInt(address, 16);
        int to_rmw_tag_decimal;
        if (L2_Assoc > 1) {
            if (Replacement_algo == "LRU"){
                to_rmw_tag_decimal =  L2_SET_LRU_req_list[index_decimal].getLast();
                L2_SET_LRU_req_list[index_decimal].removeLast();
            } else {
                to_rmw_tag_decimal =  L2_SET_FIFO_req_list[index_decimal].getLast();
                L2_SET_FIFO_req_list[index_decimal].removeLast();
            }
            for (int k = 0; k < L2_Assoc; k++) {
                if (L2_Cache_data[index_decimal][k][0] == to_rmw_tag_decimal){
                    if(L2_Cache_data[index_decimal][k][2] == 1) {
                        l2_write_backs += 1;
                        //System.out.println("L2 victim: " + replaceLastCharWithZero(Integer.toHexString(L2_Cache_data[index_decimal][k][3])) + " (tag " + Integer.toHexString(to_rmw_tag_decimal) + ", index " + index_decimal + ", dirty)");
                    } else {
                        //System.out.println("L2 victim: " + replaceLastCharWithZero(Integer.toHexString(L2_Cache_data[index_decimal][k][3])) + " (tag " + Integer.toHexString(to_rmw_tag_decimal) + ", index " + index_decimal + ", clean)");
                    }
                    if (Inclusivity == "inclusive"){
                        L1_maintain_inclusive(Integer.toHexString(L2_Cache_data[index_decimal][k][3]), L1_Assoc);
                    }
//                  Allocate New
                    L2_Cache_data[index_decimal][k] = new int[] {tag_decimal,0,0,decimalValue};
                }
            }
        } else if (L2_Assoc == 1) {
            if (L2_Cache_data[index_decimal][0][2] == 1) {
                //System.out.println("L2 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][0][3])) + " (tag " + Integer.toHexString(L1_Cache_data[index_decimal][0][0]) + ", index " + index_decimal + ", dirty)");
                l2_write_backs += 1;
                String hexadecimal = Integer.toHexString(L2_Cache_data[index_decimal][0][3]);
            } else{
                //System.out.println("L2 victim: " + replaceLastCharWithZero(Integer.toHexString(L1_Cache_data[index_decimal][0][3])) + " (tag " + Integer.toHexString(L1_Cache_data[index_decimal][0][0]) + ", index " + index_decimal + ", clean)");
            }
        }
    }
    static void L1_maintain_inclusive(String address, int assoc){
        int decimalValue = Integer.parseInt(address, 16);
        String binary_number = Integer.toBinaryString(decimalValue);

        String tag_value = binary_number.substring(0,(int) (binary_number.length() -  (L1_index_bits + L1_offset_bits)));
        int tag = Integer.parseInt(tag_value, 2);
        int index;
        if (L1_index_bits != 0){
            String index_value = binary_number.substring((int) (binary_number.length() - (L1_index_bits + L1_offset_bits)), (int) (binary_number.length() - L1_offset_bits));
            index = Integer.parseInt(index_value, 2);
        } else {
            index = 0;
        }
        if (assoc > 0){
            for (int i = 0; i < assoc; i++) {
                if (L1_Cache_data[index][i][0] == tag) {
                    if (L1_Cache_data[index][i][2] == 1){
                        //System.out.println("L1 invalidated: " + replaceLastCharWithZero(address) + " (tag " + Integer.toHexString(tag) + ", index " + index + ", dirty)");
                        //System.out.println("L1 writeback to main memory directly");
//                       Write to main memory
                        memory_writeback += 1;
                        if (L2_SET_LRU_req_list[index].contains(tag)){
                            L2_SET_LRU_req_list[index].remove((Integer) tag);
                        }
                        if (L2_SET_FIFO_req_list[index].contains(tag)){
                            L2_SET_FIFO_req_list[index].remove((Integer) tag);
                        }
                    }
                    L1_Cache_data[index][i] = new int[]{0,0,0,0};
                }
            }
        }
    }
    static void L2_LRU_Replacement(String address,int tag_decimal, int index_decimal){
       //System.out.println("L2 update LRU");
        int decimal_address = Integer.parseInt(address, 16);
        if (L2_Assoc > 1) {
            if (L2_SET_LRU_req_list[index_decimal].contains(tag_decimal)){
                L2_SET_LRU_req_list[index_decimal].remove((Integer) tag_decimal);
            } else if (L2_SET_LRU_req_list[index_decimal].size() == L2_Assoc){
                L2_SET_LRU_req_list[index_decimal].removeLast();
            }
            L2_SET_LRU_req_list[index_decimal].addFirst(tag_decimal);
        }
    }
    static void L2_FIFO_Replacement(String address,int tag_decimal, int index_decimal) {
        int decimal_address = Integer.parseInt(address, 16);
        if (L2_Assoc > 1) {
            if (!L2_SET_FIFO_req_list[index_decimal].contains(tag_decimal)) {
                L2_SET_FIFO_req_list[index_decimal].addFirst(tag_decimal);
            }
        }
    }
}
