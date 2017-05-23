library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library work;
  use work.jor.all;

entity tb_stack is
end tb_stack;

architecture tb of tb_stack is

    component stack
        port (i : in stack_in;
              o : out stack_out);
    end component;

    signal i : stack_in;
    signal o : stack_out;

    constant tb_period : time := 100 ns;
    signal tb_clk : std_logic := '0';
    signal tb_end : std_logic := '0';

begin

    dut : stack
    port map (i => i,
              o => o);

    -- Clock generation
    tb_clk <= not tb_clk after tb_period/2 when tb_end /= '1' else '0';

    i.clk <= tb_clk;

    stimuli : process
    begin
        i.data.cclc <= (others => '0');
        i.data.cop  <= (others => '0');
        i.data.cr   <= (others => '0');
        i.push      <= '0';
        i.pop       <= '0';

        -- Reset
        i.rst <= '1';
        wait for tb_period;
        i.rst <= '0';
        wait for tb_period;

        -- Attempt to pop while empty
        i.data.cclc <= (others => '0');
        i.data.cop  <= (others => '0');
        i.data.cr   <= (others => '0');
        i.push      <= '0';
        i.pop       <= '1';
        wait for tb_period;
        
        -- Push an item
        i.data.cclc <= std_logic_vector(to_unsigned(1,JOR_CCL_ADDR_BITS));
        i.data.cop  <= std_logic_vector(to_unsigned(2,JOR_LC_ADDR_BITS));
        i.data.cr   <= std_logic_vector(to_unsigned(3,JOR_HOST_ADDR_BITS));
        i.push      <= '1';
        i.pop       <= '0';
        wait for tb_period;
        
        -- Pop that item;
        i.data.cclc <= (others => '0');
        i.data.cop  <= (others => '0');
        i.data.cr   <= (others => '0');
        i.push      <= '0';
        i.pop       <= '1';
        wait for tb_period;
        
        -- Push the stack until full
        for K in 0 to JOR_STACK_SIZE-1 loop
          i.data.cclc <= std_logic_vector(to_unsigned(K,JOR_CCL_ADDR_BITS));
          i.data.cop  <= std_logic_vector(to_unsigned(K,JOR_LC_ADDR_BITS));
          i.data.cr   <= std_logic_vector(to_unsigned(K,JOR_HOST_ADDR_BITS));
          i.push      <= '1';
          i.pop       <= '0';
          wait for tb_period;
        end loop;
          
        -- Attempt to push an item onto a full stack
        i.data.cclc <= std_logic_vector(to_unsigned(4,JOR_CCL_ADDR_BITS));
        i.data.cop  <= std_logic_vector(to_unsigned(5,JOR_LC_ADDR_BITS));
        i.data.cr   <= std_logic_vector(to_unsigned(6,JOR_HOST_ADDR_BITS));
        i.push      <= '1';
        i.pop       <= '0';
        wait for tb_period;
        
        -- Pop stack until empty
        for K in 0 to JOR_STACK_SIZE-1 loop
          i.data.cclc <= (others => '0');
          i.data.cop  <= (others => '0');
          i.data.cr   <= (others => '0');
          i.push      <= '0';
          i.pop       <= '1';
          wait for tb_period;
        end loop;                

        tb_end <= '1';
        wait;
    end process;

end tb;
