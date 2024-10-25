async function loadCSV() {
    try {
        const response = await fetch('gamehistory.csv');
        const data = await response.text();
        const rows = data.trim().split('\n');
        
        let tableHTML = '<table><tbody>';
        
        rows.forEach(row => {
            tableHTML += '<tr>';
            const cells = row.split(',');
            cells.forEach(cell => {
                const result = cell.trim();
                tableHTML += `<td class="${result}">${result}</td>`;
            });
            tableHTML += '</tr>';
        });
        
        tableHTML += '</tbody></table>';
        document.getElementById('tableContainer').innerHTML = tableHTML;
    } catch (error) {
        console.error('Error loading CSV:', error);
        document.getElementById('tableContainer').innerHTML = 'Error loading data';
    }
}

loadCSV();