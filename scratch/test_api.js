async function test() {
  try {
    const res = await fetch('http://localhost:3000/api/stores', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'TestStore_NativeFetch' })
    });
    const data = await res.json();
    console.log('Success:', data);
  } catch (err) {
    console.error('Error:', err);
  }
}

test();
