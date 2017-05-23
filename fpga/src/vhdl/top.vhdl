library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;
  
library work;
  use work.jor.all;

entity top is
  port (
    clk   : in    std_logic;
    rst   : in    std_logic;
    
    start : in    std_logic;
    busy  : out   std_logic;
    done  : out   std_logic;
    error : out   std_logic;
    
    init_cr : unsigned(JOR_HOST_ADDR_BITS-1 downto 0)
  );
end entity;

architecture rtl of top is
  signal stack_i : stack_in;
  signal stack_o : stack_out;
  
  signal ccl_i : ccl_in;
  signal ccl_o : ccl_out;
  
  signal r : jor_regs := jor_regs_init;

  signal state : jor_state;
  
  signal d : jor_regs;
    
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
  
  stack_i.clk       <= clk;
  stack_i.rst       <= rst;
  stack_i.pop       <= d.stack.pop;
  stack_i.push      <= d.stack.push;
  stack_i.data.cclc <= std_logic_vector(r.cclc+1);
  stack_i.data.cop  <= std_logic_vector(r.cop);
  stack_i.data.cr   <= std_logic_vector(r.cr);
    
  -- Instruction decode logic
  sm: process(all)
    variable q : jor_regs;
  begin
    -- Copy old register values into variable
    q := r;
    q.stack.push  := '0';
    q.stack.pop   := '0';
    q.instr       := ccl_o.dob;

    -- Check instruction type
    -- Class instructions have their MSB set to 0
    if q.instr(JOR_CLASS_BIT) = '0' then
      report "Class";
      -- The instruction word is also the size of the class
      -- Update the current object size
      q.cos             := unsigned(r.instr(JOR_LC_ADDR_BITS-1 downto 0));
      -- Accumulate tail register
      q.tail            := r.tail + unsigned(r.instr(JOR_LC_ADDR_BITS-1 downto 0));
      -- Increment cclc register
      q.cclc            := q.cclc + 1;
    else
    -- Other instruction, decode first:
      case q.instr(JOR_CCL_INSTR_SIZE-1 downto JOR_CCL_INSTR_SIZE-3) is
      
        when JOR_REFE => -- Reference
          report "Reference";
          if (stack_o.full = '0') then              -- Check if stack is not full
            q.stack.push := '1';                    -- Push regs to stack
            q.cop       := q.cop + q.cos;           -- Accumulate current object pointer
            -- Jump to class:
            q.cclc      := unsigned(q.instr(JOR_REFE_INDEX_TOP downto JOR_REFE_INDEX_BOT)); 
          else 
            error       <= '1';                       -- Stack is full, error
          end if;
          
        when JOR_ATYP => -- Array of primitive types
          report "Array of primitives";
          q.cclc        := q.cclc + 1;                -- Increment cclc register
          
        when JOR_AOBJ => -- Array of objects
          report "Array of objects";
          q.cclc        := q.cclc + 1;                -- Increment cclc register
          
        when JOR_EOCL => -- End of Class
          report "End of class";
          if (stack_o.empty = '0') then
            q.stack.pop := '1';
            q.cop       := unsigned(stack_o.data.cop);
            q.cclc      := unsigned(stack_o.data.cclc);
            q.cr        := unsigned(stack_o.data.cr);
          else
            error       <= '1';                       -- Stack is empty, error
          end if;
          
        when others   => -- Invalid
          report "Unknown";
          q.cclc        := q.cclc + 1;                -- Increment cclc register
          error         <= '1';                       -- Stack is empty, error
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
