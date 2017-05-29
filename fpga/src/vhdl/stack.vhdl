library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;
  use work.jor.all;

entity stack is
  port (
    i : in  stack_in;
    o : out stack_out
  );
end entity;

architecture rtl of stack is
  type stack_array is array(0 to JOR_STACK_SIZE-1) of stack_item;
  signal items : stack_array;
  signal index : integer range 0 to JOR_STACK_SIZE := 0;
  signal empty : std_logic := '0';
  signal full  : std_logic := '0';
begin

  -- Empty and full signals:
  with index select empty <=
    '1' when 0,
    '0' when others;

  with index select full <=
    '1' when JOR_STACK_SIZE,
    '0' when others;

  o.empty <= empty;
  o.full  <= full;

  -- Get output from index:
  with index select o.data <=
    items(0)       when 0,
    items(index-1) when others;

  -- Popping and pushing:
  process(i.clk)
  begin
    if rising_edge(i.clk) then
      if i.rst = '1' then
        index <= 0;
      else
        if i.pop = '1' and empty = '0' then
          index <= index - 1;
        end if;
        if i.push = '1' and full = '0' then
          items(index) <= i.data;
          index <= index + 1;
        end if;
      end if;
    end if;
  end process;

end architecture;
