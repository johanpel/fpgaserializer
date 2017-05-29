library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;

package utils is
  function bs64(word : in std_logic_vector(63 downto 0)) return std_logic_vector;
  function bs32(word : in std_logic_vector(31 downto 0)) return std_logic_vector;
  function bs16(word : in std_logic_vector(15 downto 0)) return std_logic_vector;
end package;


package body utils is
  function bs64(word : in std_logic_vector(63 downto 0)) return std_logic_vector is
  begin
    return word( 7 downto  0) &
           word(15 downto  8) &
           word(23 downto 16) &
           word(31 downto 24) &
           word(39 downto 32) &
           word(47 downto 40) &
           word(55 downto 48) &
           word(63 downto 56);
  end;

  function bs32(word : in std_logic_vector(31 downto 0)) return std_logic_vector is
  begin
    return word( 7 downto  0) &
           word(15 downto  8) &
           word(23 downto 16) &
           word(31 downto 24);
  end;

  function bs16(word : in std_logic_vector(15 downto 0)) return std_logic_vector is
  begin
    return word( 7 downto  0) &
           word(15 downto  8);
  end;

end utils;
