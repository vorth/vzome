
import { simplify3, createField } from './common.js'

const SQRT3 = Math.sqrt(3)

function reciprocal( x )
{
  const [ a=0n, b=0n, c=1n ] = x;
  return simplify3( a*c, 0n - b*c, a*a - 3n*b*b )
}

function times( a, b )
{
  const [ a0=0n, a1=0n, ad=1n ] = a, [ b0=0n, b1=0n, bd=1n ] = b
  return simplify3( a0*b0 + 3n*a1*b1, a0*b1 + a1*b0, ad*bd )
}

function embed( a )
{
  const [ a0=0n, a1=0n, ad=1n ] = a
  return ( Number(a0) + SQRT3 * Number(a1) ) / Number(ad)
}

const getIrrational = () => '√3';

const field = { ...createField( { name: 'rootThree', order: 2, times, embed, reciprocal } ), getIrrational }

export default field
