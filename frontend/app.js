// Cliente GraphQL del frontend. Todas las peticiones van a /graphql (el
// servidor Python hace proxy al backend en localhost:8080).

const ENDPOINT = '/graphql';

const QUERY_HISTORIAS = `
  query Historias($identification: String!, $first: Int, $after: String) {
    clinicalHistoriesByIdentification(
      identification: $identification, first: $first, after: $after
    ) {
      nodes {
        id
        consultationReason
        diagnosis
        treatment
        date
        patient { identification firstName lastName }
        doctor { name specialty }
      }
      pageInfo { hasNextPage endCursor }
    }
  }
`;

const MUTACION_REGISTRO = `
  mutation Crear($input: CreateClinicalHistoryInput!) {
    createClinicalHistory(input: $input) { id }
  }
`;

let estado = { cedula: '', first: 10, cursor: null };

async function gql(query, variables) {
  const respuesta = await fetch(ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, variables })
  });
  const cuerpo = await respuesta.json();
  if (cuerpo.errors && cuerpo.errors.length) {
    throw new Error(cuerpo.errors.map(e => e.message).join(' | '));
  }
  return cuerpo.data;
}

function mostrarEstado(id, mensaje, tipo) {
  const el = document.getElementById(id);
  el.textContent = mensaje;
  el.className = 'estado ' + tipo;
  el.hidden = false;
}

function cedulaValida(valor) {
  return /^[A-Za-z0-9-]{4,30}$/.test(valor.trim());
}

async function consultar(cedula, first, after, reemplazar) {
  const datos = await gql(QUERY_HISTORIAS, {
    identification: cedula,
    first: first,
    after: after || null
  });

  const connection = datos.clinicalHistoriesByIdentification;
  pintarHistorias(connection.nodes, reemplazar);

  document.getElementById('resultados').hidden = false;
  document.getElementById('total').textContent = String(connection.nodes.length);
  estado.cursor = connection.pageInfo.endCursor;
  document.getElementById('btn-mas').hidden = !connection.pageInfo.hasNextPage;
  return connection.nodes.length;
}

function pintarHistorias(nodes, reemplazar) {
  const lista = document.getElementById('lista');
  if (reemplazar) lista.replaceChildren();

  if (nodes.length === 0 && reemplazar) {
    const p = document.createElement('p');
    p.className = 'vacio';
    p.textContent = 'No hay historias para esta cédula.';
    lista.appendChild(p);
  }

  nodes.forEach(h => {
    const card = document.createElement('article');
    card.className = 'historia';

    const titulo = document.createElement('h4');
    titulo.textContent = `${h.diagnosis} — ${h.date}`;
    card.appendChild(titulo);

    [
      ['Paciente', `${h.patient.firstName} ${h.patient.lastName} (${h.patient.identification})`],
      ['Doctor', `${h.doctor.name} — ${h.doctor.specialty}`],
      ['Motivo', h.consultationReason],
      ['Tratamiento', h.treatment]
    ].forEach(([etiqueta, valor]) => {
      const p = document.createElement('p');
      const strong = document.createElement('strong');
      strong.textContent = etiqueta + ': ';
      p.appendChild(strong);
      p.appendChild(document.createTextNode(valor));
      card.appendChild(p);
    });

    const id = document.createElement('p');
    id.className = 'meta';
    id.textContent = 'ID: ' + h.id;
    card.appendChild(id);

    lista.appendChild(card);
  });
}

document.getElementById('form-consulta').addEventListener('submit', async (e) => {
  e.preventDefault();
  const cedula = document.getElementById('cedula-consulta').value.trim();
  const first = Number(document.getElementById('first').value);

  if (!cedulaValida(cedula)) {
    mostrarEstado('consulta-estado',
      'La cédula debe tener entre 4 y 30 caracteres alfanuméricos o guiones.', 'error');
    return;
  }

  estado = { cedula, first, cursor: null };
  try {
    const n = await consultar(cedula, first, null, true);
    mostrarEstado('consulta-estado', `Consulta exitosa: ${n} historia(s).`, 'ok');
  } catch (error) {
    mostrarEstado('consulta-estado', 'Error: ' + error.message, 'error');
    document.getElementById('resultados').hidden = true;
  }
});

document.getElementById('btn-mas').addEventListener('click', async () => {
  if (!estado.cursor) return;
  try {
    await consultar(estado.cedula, estado.first, estado.cursor, false);
    mostrarEstado('consulta-estado', 'Página siguiente cargada.', 'ok');
  } catch (error) {
    mostrarEstado('consulta-estado', 'Error: ' + error.message, 'error');
  }
});

document.getElementById('form-registro').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  if (!form.checkValidity()) {
    mostrarEstado('registro-estado', 'Revisa los campos obligatorios del formulario.', 'error');
    return;
  }

  const v = new FormData(form);
  const cedula = String(v.get('identification')).trim();
  const edad = Number(v.get('age'));

  if (!cedulaValida(cedula)) {
    mostrarEstado('registro-estado', 'Cédula inválida (4-30 caracteres alfanuméricos o guiones).', 'error');
    return;
  }
  if (edad < 0 || edad > 150) {
    mostrarEstado('registro-estado', 'La edad debe estar entre 0 y 150.', 'error');
    return;
  }

  const input = {
    patient: {
      identification: cedula,
      firstName: String(v.get('firstName')).trim(),
      lastName: String(v.get('lastName')).trim(),
      age: edad,
      gender: v.get('gender')
    },
    doctor: {
      name: String(v.get('doctorName')).trim(),
      professionalLicense: String(v.get('professionalLicense')).trim(),
      specialty: String(v.get('specialty')).trim()
    },
    history: {
      consultationReason: String(v.get('consultationReason')).trim(),
      diagnosis: String(v.get('diagnosis')).trim(),
      treatment: String(v.get('treatment')).trim(),
      date: new Date(String(v.get('date'))).toISOString()
    }
  };

  try {
    const datos = await gql(MUTACION_REGISTRO, { input });
    mostrarEstado('registro-estado',
      'Historia registrada con ID ' + datos.createClinicalHistory.id, 'ok');
    document.getElementById('cedula-consulta').value = cedula;
    await consultar(cedula, Number(document.getElementById('first').value), null, true);
  } catch (error) {
    mostrarEstado('registro-estado', 'Error: ' + error.message, 'error');
  }
});
