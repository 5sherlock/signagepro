async function debug() {
  try {
    const res = await fetch('http://localhost:3000/api/devices');
    const data = await res.json();
    console.log('--- Raw Devices JSON ---');
    console.log(JSON.stringify(data, null, 2));
  } catch (err) {
    console.error('Fetch error:', err);
  }
}
debug();
