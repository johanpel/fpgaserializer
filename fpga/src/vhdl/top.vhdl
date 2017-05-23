library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;
  
library work;
  use work.jor.all;

entity top is
  port (   
    start : in    std_logic;
    busy  : out   std_logic;
    done  : out   std_logic;
    error : out   std_logic;
    
    init_cr : unsigned(JOR_HOST_ADDR_BITS-1 downto 0);
    
    dh_o_tb : in data_handler_out;
    dh_i_tb : out data_handler_in;

    clk   : in    std_logic;
    rst   : in    std_logic
  );
end entity;

architecture rtl of top is
    
  signal stack_i      : stack_in;
  signal stack_o      : stack_out;
    
  signal ccl_i        : ccl_ram_in;
  signal ccl_o        : ccl_ram_out;
    
  signal lc_i         : lc_ram_in;
  signal lc_o         : lc_ram_out;
    
  signal dh_i         : data_handler_in;
  signal dh_o         : data_handler_out;
    
  signal r            : jor_regs := jor_regs_init; 
  signal d            : jor_regs;
    
  signal array_size   : unsigned(JOR_ARRAY_SIZE_BITS + JOR_A_ELEMENT_SIZE_BITS - 1 downto 0) := (others => '0');
  signal instruction  : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0) := (others => '0');
  signal ret          : unsigned(JOR_CCL_ADDR_BITS-1 downto 0) := (others => '0');
begin

  -- Stack
  stack: entity work.stack port map (
    i => stack_i,
    o => stack_o
  );
  
  -- CCL RAM that holds the instructions
  cclram: entity work.ccl_ram 
    generic map (
      CCL_RAM_INIT     => CCL_RAM_TESTBENCH
    )
    port map (
      clka  => ccl_i.clka,
      clkb  => ccl_i.clkb,
      ena   => ccl_i.ena,
      enb   => ccl_i.enb, 
      wea   => ccl_i.wea, 
      addra => ccl_i.addra,
      addrb => ccl_i.addrb,
      dia   => ccl_i.dia,
      dob   => ccl_o.dob
    );  

  -- Connect CCL RAM Port A stuff
  ccl_i.clka        <= clk;
  ccl_i.ena         <= '0';
  ccl_i.wea         <= '0';
  ccl_i.addra       <= (others => '0');
  ccl_i.dia         <= (others => '0');
    
  -- Connect CCL RAM Port B address to the CCL Counter
  ccl_i.addrb       <= std_logic_vector(r.cclc);
  ccl_i.enb         <= '1';
  ccl_i.clkb        <= clk;
   
  -- LC RAM that holds the local copy of the object
  lcram: entity work.lc_ram 
    generic map (
      LC_RAM_INIT     => LC_RAM_TESTBENCH
    )
    port map (
      clka  => lc_i.clka,
      clkb  => lc_i.clkb,
      ena   => lc_i.ena,
      enb   => lc_i.enb, 
      wea   => lc_i.wea, 
      addra => lc_i.addra,
      addrb => lc_i.addrb,
      dia   => lc_i.dia,
      dob   => lc_o.dob
    );
  
  -- Connect LC RAM Port A stuff
  lc_i.clka         <= clk;
  lc_i.ena          <= '0';
  lc_i.wea          <= '0';
  lc_i.addra        <= (others => '0');
  lc_i.dia          <= (others => '0');
    
  -- Connect LC RAM Port B address to the CCL Counter
  lc_i.addrb        <= r.comp.lc.addr;
    
  stack_i.clk       <= clk;
  stack_i.rst       <= rst;
  stack_i.pop       <= d.comp.stack.pop;
  stack_i.push      <= d.comp.stack.push;
  stack_i.data.cclc <= std_logic_vector(ret);
  stack_i.data.cop  <= std_logic_vector(r.cop);
  stack_i.data.cr   <= std_logic_vector(r.cr);
  
  dh_i              <= r.comp.dh;
  
  -- For testbench:
  dh_o              <= dh_o_tb;
  dh_i_tb           <= dh_i;
  
  -- Array size multiplier:
  -- TODO: replace this with shift left log2(element size)?
  array_size        <= unsigned(instruction(JOR_A_ELEMENT_SIZE_HI downto JOR_A_ELEMENT_SIZE_LO))
                       *
                       unsigned(lc_o.dob(JOR_ARRAY_SW_HI downto JOR_ARRAY_SW_LO));
    
  -- Instruction from CCL:
  instruction       <= ccl_o.dob;
      
  -- Instruction processing logic
  sm: process(all)
    variable q : jor_regs;
  begin
    -- Copy old register values into variable
    q := r;
    q.comp.stack.push := '0';
    q.comp.stack.pop  := '0';
    q.comp.dh.valid   := '0';
------------------------------------------ CLAS instruction ---------------------------------------
    -- Class instructions have their MSB set to 0
    if instruction(JOR_CLASS_BIT) = '0' then
      -- The instruction word is also the size of the class
      q.cos               := unsigned(instruction(JOR_LC_ADDR_BITS-1 downto 0));

      if r.stall = '0' then                                                                         -- Check if we are not already requesting
        q.comp.dh.source  := std_logic_vector(r.cr);                                                -- Location of the object in the host memory
        q.comp.dh.dest    := std_logic_vector(r.tail);                                              -- Location of the object in the local copy
        q.comp.dh.size    := std_logic_vector(q.cos);                                               -- Size of the object
        q.comp.dh.id	    := std_logic_vector(r.reqid);                                             -- ID of the request
        q.comp.dh.valid   := '1';                                                                   -- Request the data from the data handler
        
        q.reqid           := r.reqid + 1;                                                           -- Increment request ID for the next request
        q.stall           := '1';                                                                   -- Wait for the request to complete
      end if;

      if r.stall = '1' and dh_o.id = r.comp.dh.id and dh_o.done = '1' then                          -- Check if the data handler is done for the current id
                                                                                                    -- TODO: this could potentially be moved to the REF instruction
        q.tail            := r.tail + unsigned(instruction(JOR_LC_ADDR_BITS-1 downto 0));           -- Accumulate tail register
        q.stall           := '0';                                                                   -- Continue
        q.cclc            := r.cclc + 1;                                                            -- Increment cclc register
      end if;
    else
---------------------------------------------------------------------------------------------------

    -- Other instruction, decode first:
      case instruction(JOR_CCL_INSTR_SIZE-1 downto JOR_CCL_INSTR_SIZE-3) is

------------------------------------------ REFE instruction ---------------------------------------
        when JOR_REFE => 
          if (stack_o.full = '0') then                                                              -- Check if stack is not full
            q.comp.stack.push := '1';                                                               -- Push regs to stack
            q.cop := r.cop + r.cos;                                                                 -- Accumulate current object pointer
            ret <= r.cclc + 1;                                                                      -- Return index is index + 1
            q.cclc := unsigned(instruction(JOR_REFE_INDEX_HI downto JOR_REFE_INDEX_LO));            -- Jump to class
          else 
            error <= '1';                                                                           -- Stack is full, error
          end if;
---------------------------------------------------------------------------------------------------

------------------------------------------ ATYP instruction ---------------------------------------
        when JOR_ATYP =>                                                                            -- Array of primitive types
          -- Obtain the array header
          if r.stall = '0' and r.array_stall = '0' then                                             -- Check if we are not already in a further step of this instruction
            q.comp.dh.source  := std_logic_vector(r.cr);                                            -- Location of the array in the host memory
            q.comp.dh.dest    := std_logic_vector(r.tail);                                          -- Location of the array in the local copy
            q.comp.dh.size    := std_logic_vector(to_unsigned(JOR_ARRAY_HEADER_SIZE,
                                                              JOR_OBJ_SIZE_BITS));                  -- Size of the array header
            q.comp.dh.id	    := std_logic_vector(r.reqid);                                         -- ID of the request
            q.comp.dh.valid   := '1';                                                               -- Request the data from the data handler
            
            q.reqid           := r.reqid + 1;                                                       -- Increment request ID for the next request
            q.stall           := '1';                                                               -- Wait for the request to complete
            
            q.comp.lc.addr    := std_logic_vector(unsigned(q.cop) + JOR_ARRAY_SIZE_WORD_OFFSET);    -- Set the address of array size word for the local copy RAM
          end if;
          
          -- Obtain the array elements
          if r.stall = '1' and dh_o.id = r.comp.dh.id and dh_o.done = '1' then                      -- Check if the data handler is done for the current id
            q.stall           := '0';                                                               -- Normal stall done
            q.tail            := r.tail + JOR_ARRAY_HEADER_SIZE;                                    -- Increase tail
            q.comp.dh.source  := std_logic_vector(r.cr+JOR_ARRAY_HEADER_SIZE);                      -- Location of the array elements in the host memory
            q.comp.dh.dest    := std_logic_vector(r.tail);                                          -- Location of the array elements in the local copy
            q.comp.dh.size    := std_logic_vector(array_size(JOR_OBJ_SIZE_BITS-1 downto 0));        -- Size of the elements times number of elements
            q.comp.dh.id	    := std_logic_vector(r.reqid);                                         -- ID of the request
            q.comp.dh.valid   := '1';                                                               -- Request the data from the data handler
            
            q.reqid           := r.reqid + 1;                                                       -- Increment request ID for the next request
            q.array_stall     := '1';                                                               -- Wait for the request to complete
          end if;
                    
          if r.array_stall = '1' and dh_o.id = r.comp.dh.id and dh_o.done = '1' then                -- Check if the data handler is done for the current id
            q.tail            := r.tail + array_size(JOR_OBJ_SIZE_BITS-1 downto 0);                 -- Accumulate tail
            q.array_stall     := '0';                                                               -- Continue
            q.cclc            := r.cclc + 1;                                                        -- Increment cclc register
          end if;
---------------------------------------------------------------------------------------------------

------------------------------------------ AOBJ instruction ---------------------------------------
        when JOR_AOBJ =>                                                                            -- Array of objects
          -- Obtain the array header 
          if r.stall = '0' 
            and r.array_stall = '0' 
            and r.oa.act = '0' 
          then                                                                                      -- Check if we are not already in a further step of this instruction
            q.comp.dh.source  := std_logic_vector(r.cr);                                            -- Location of the array in the host memory
            q.comp.dh.dest    := std_logic_vector(r.tail);                                          -- Location of the array in the local copy
            q.comp.dh.size    := std_logic_vector(to_unsigned(JOR_ARRAY_HEADER_SIZE,
                                                              JOR_OBJ_SIZE_BITS));                  -- Size of the array header
            q.comp.dh.id	    := std_logic_vector(r.reqid);                                         -- ID of the request
            q.comp.dh.valid   := '1';                                                               -- Request the data from the data handler
            
            q.reqid           := r.reqid + 1;                                                       -- Increment request ID for the next request
            q.stall           := '1';                                                               -- Wait for the request to complete
            
            q.comp.lc.addr    := std_logic_vector(unsigned(q.cop) + JOR_ARRAY_SIZE_WORD_OFFSET);    -- Set the address of array size word for the local copy RAM
          end if;
          
          -- Obtain the array elements
          if r.stall = '1' 
            and dh_o.id = r.comp.dh.id 
            and dh_o.done = '1' 
            and r.oa.act = '0' 
          then                                                                                      -- Check if the data handler is done for the current id
            q.stall           := '0';                                                               -- Normal stall done
            q.tail            := r.tail + JOR_ARRAY_HEADER_SIZE;                                    -- Increase tail
            q.comp.dh.source  := std_logic_vector(r.cr+JOR_ARRAY_HEADER_SIZE);                      -- Location of the array elements in the host memory
            q.comp.dh.dest    := std_logic_vector(r.tail);                                          -- Location of the array elements in the local copy
            q.comp.dh.size    := std_logic_vector(array_size(JOR_OBJ_SIZE_BITS-1 downto 0));        -- Size of the elements times number of elements
            q.comp.dh.id	    := std_logic_vector(r.reqid);                                         -- ID of the request
            q.comp.dh.valid   := '1';                                                               -- Request the data from the data handler
                                   
            q.reqid           := r.reqid + 1;                                                       -- Increment request ID for the next request
            q.array_stall     := '1';                                                               -- Wait for the request to complete
            
            q.oa.cas := array_size(JOR_OBJ_SIZE_BITS-1 downto 0);                                   -- Set current array size
            q.oa.cac := (others => '0');                                                            -- Reset current array index
          end if;
          
          -- After obtaining array elements, switch to handling object array contents
          -- TODO: merge with next step somehow
          if r.array_stall = '1' 
            and dh_o.id = r.comp.dh.id 
            and dh_o.done = '1' 
            and r.oa.act = '0' 
          then                                                                                      -- Check if the data handler is done for the current id
            if array_size /= JOR_ARRAY_SIZE_EMPTY then                                              -- Check if we're not dealing with an empty array
              q.oa.act           := '1';                                                            -- Set object array mode to active
            else
              q.cclc            := r.cclc + 1;                                                      -- Empty array, we're done. Increment cclc register
            end if;
            q.tail            := r.tail + array_size(JOR_OBJ_SIZE_BITS-1 downto 0);                 -- Accumulate tail
            q.array_stall     := '0';                                                               -- Continue
          end if;
         
          -- Handle object array elements
          -- Involves pushing to the stack for every array element until we've done all of them
          if r.array_stall = '0'
            and r.stall = '0'
            and r.oa.act = '1'
          then
            if stack_o.full = '0'                                                                   -- Check if stack is not full
            then                       
                if r.oa.cac = r.oa.cas-1 then                                                       -- Check if this is the last element
                  ret <= r.cclc + 1;                                                                -- The next instruction is the return index. This should be an EOCL
                  q.oa.act := '0';                                                                  -- Deactivate object array mode
                else
                  ret <= r.cclc;                                                                    -- This instruction is the return index
                end if;
                q.oa.cac := r.oa.cac + 1;                                                           -- Increase array index for element
                q.comp.lc.addr := std_logic_vector(unsigned(q.cop) + r.oa.cac);                     -- Select the reference in the local copy
                q.cr := unsigned(lc_o.dob);                                                         -- New ref we're working on from
                
                q.comp.stack.push := '1';                                                           -- Push regs to stack
                
                q.cclc := unsigned(instruction(JOR_REFE_INDEX_HI downto JOR_REFE_INDEX_LO));        -- Jump to class
              else                                                                                  
                error <= '1';                                                                       -- Stack is full, error
              end if;
          end if;
---------------------------------------------------------------------------------------------------

------------------------------------------ EOCL instruction ---------------------------------------
        when JOR_EOCL =>                                                                            -- End of Class
          if (stack_o.empty = '0') then
            q.comp.stack.pop := '1';
            q.cop := unsigned(stack_o.data.cop);
            q.cclc := unsigned(stack_o.data.cclc);
            q.cr := unsigned(stack_o.data.cr);
          else
            error <= '1';                                                                           -- Stack is empty, error
          end if;
---------------------------------------------------------------------------------------------------

        when others => -- Invalid
          q.cclc := r.cclc + 1;                                                                     -- Increment cclc register
          error <= '1';                                                                             -- Stack is empty, error
      end case;
    end if;
    d <= q;
  end process;

  regs: process(clk)
  begin
    if rising_edge(clk) then
      if rst = '1' then
        r <= jor_regs_init;
      else
        r <= d;
      end if;
    end if;
  end process;
  
  
end architecture;
