import {createLazyFileRoute, useNavigate} from '@tanstack/react-router';

export const Route = createLazyFileRoute('/bill/')({
    component: Index
});

function Index() {
    const navigation = useNavigate();
    navigation({to: '/'});
    return;
}
