function f(data: unknown): void
{
  if(typeof data === 'number') hostIndex = data;
  else                         [hostIndex, hostDirectivesStart, hostDirectivesEnd] = data;
}
